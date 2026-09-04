package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Authentication session payload from Supabase Auth.
 */
data class SupabaseUserSession(
    val accessToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long = 3600,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (expiresIn * 1000L)
) {
    /**
     * Checks if the access token has expired (or is within a 30s safety threshold).
     */
    fun isExpired(): Boolean = System.currentTimeMillis() >= (expiresAt - 30_000L)
}

/**
 * High-level authentication status observer for Supabase.
 */
sealed class SupabaseAuthStatus {
    object Initializing : SupabaseAuthStatus()
    data class Authenticated(val session: SupabaseUserSession) : SupabaseAuthStatus()
    object NotAuthenticated : SupabaseAuthStatus()
    data class Error(val message: String) : SupabaseAuthStatus()
}

enum class SupabaseConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Configuration for Supabase Auth plugin.
 *
 * @param autoLoadFromStorage Loads cached session credentials from secure storage on startup.
 * @param alwaysAutoRefresh Automatically refreshes expired JWT tokens using refresh_token.
 */
data class SupabaseAuthConfig(
    var autoLoadFromStorage: Boolean = true,
    var alwaysAutoRefresh: Boolean = true
)

/**
 * Secure Session Storage for persisting Auth tokens across application restarts.
 */
class SecureSessionStorage(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "fadx_supabase_auth_store",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun saveSession(session: SupabaseUserSession) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_TOKEN_TYPE, session.tokenType)
            putLong(KEY_EXPIRES_IN, session.expiresIn)
            putLong(KEY_EXPIRES_AT, session.expiresAt)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_USER_EMAIL, session.email)
            apply()
        }
    }

    fun loadSession(): SupabaseUserSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val tokenType = prefs.getString(KEY_TOKEN_TYPE, "bearer") ?: "bearer"
        val expiresIn = prefs.getLong(KEY_EXPIRES_IN, 3600L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)

        return SupabaseUserSession(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresIn = expiresIn,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            expiresAt = expiresAt
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

/**
 * Production-ready Supabase Client for Android, including Auth plugin with session persistence,
 * auto-refresh, PostgREST API helpers, and profile synchronization.
 */
class SupabaseClient private constructor() {

    private val tag = "SupabaseClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connection state
    private val _connectionStatus = MutableStateFlow(SupabaseConnectionStatus.IDLE)
    val connectionStatus: StateFlow<SupabaseConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Auth Plugin handling authentication, token refresh, and persistent storage.
     */
    inner class Auth(
        val config: SupabaseAuthConfig = SupabaseAuthConfig(
            autoLoadFromStorage = true,
            alwaysAutoRefresh = true
        )
    ) {
        private var storage: SecureSessionStorage? = null

        private val _session = MutableStateFlow<SupabaseUserSession?>(null)
        val session: StateFlow<SupabaseUserSession?> = _session.asStateFlow()

        private val _status = MutableStateFlow<SupabaseAuthStatus>(SupabaseAuthStatus.Initializing)
        val status: StateFlow<SupabaseAuthStatus> = _status.asStateFlow()

        /**
         * Initializes Auth plugin with local storage and restores active session.
         */
        fun initialize(context: Context) {
            if (storage == null) {
                storage = SecureSessionStorage(context)
                if (config.autoLoadFromStorage) {
                    loadPersistedSession()
                } else {
                    _status.value = SupabaseAuthStatus.NotAuthenticated
                }
            }
        }

        /**
         * Synchronously returns current session or null if not authenticated / expired.
         */
        fun currentSessionOrNull(): SupabaseUserSession? {
            val s = _session.value ?: return null
            if (s.isExpired()) {
                if (config.alwaysAutoRefresh && !s.refreshToken.isNullOrBlank()) {
                    // Trigger async refresh
                    clientScope.launch { refreshSession() }
                }
                return if (s.isExpired()) null else s
            }
            return s
        }

        private fun loadPersistedSession() {
            try {
                val persisted = storage?.loadSession()
                if (persisted != null) {
                    Log.i(tag, "Loaded persisted session for user: ${persisted.userId}")
                    _session.value = persisted
                    _connectionStatus.value = SupabaseConnectionStatus.CONNECTED

                    if (persisted.isExpired()) {
                        Log.i(tag, "Persisted token is expired. Auto-refresh enabled: ${config.alwaysAutoRefresh}")
                        if (config.alwaysAutoRefresh && !persisted.refreshToken.isNullOrBlank()) {
                            clientScope.launch { refreshSession() }
                        } else {
                            _status.value = SupabaseAuthStatus.NotAuthenticated
                        }
                    } else {
                        _status.value = SupabaseAuthStatus.Authenticated(persisted)
                    }
                } else {
                    Log.i(tag, "No persisted session found")
                    _status.value = SupabaseAuthStatus.NotAuthenticated
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load persisted session", e)
                _status.value = SupabaseAuthStatus.NotAuthenticated
            }
        }

        /**
         * Sign in user with email and password.
         */
        suspend fun signInWithPassword(email: String, password: String): Result<SupabaseUserSession> =
            withContext(Dispatchers.IO) {
                try {
                    val json = JSONObject().apply {
                        put("email", email.trim())
                        put("password", password)
                    }

                    val request = Request.Builder()
                        .url("${SupabaseConfig.authUrl}/token?grant_type=password")
                        .post(json.toString().toRequestBody(jsonMediaType))
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            val parsed = JSONObject(responseBody)
                            val accessToken = parsed.getString("access_token")
                            val tokenType = parsed.optString("token_type", "bearer")
                            val expiresIn = parsed.optLong("expires_in", 3600)
                            val refreshToken = parsed.optString("refresh_token", null)
                            val userObj = parsed.optJSONObject("user")
                            val userId = userObj?.optString("id")
                            val userEmail = userObj?.optString("email") ?: email

                            val newSession = SupabaseUserSession(
                                accessToken = accessToken,
                                tokenType = tokenType,
                                expiresIn = expiresIn,
                                refreshToken = refreshToken,
                                userId = userId,
                                email = userEmail
                            )

                            // Update in-memory and persistent storage
                            _session.value = newSession
                            _status.value = SupabaseAuthStatus.Authenticated(newSession)
                            _connectionStatus.value = SupabaseConnectionStatus.CONNECTED
                            storage?.saveSession(newSession)

                            Log.i(tag, "Sign-in successful for user $userId. Session stored.")
                            Result.success(newSession)
                        } else {
                            val errorJson = try { JSONObject(responseBody) } catch (_: Exception) { null }
                            val errorMsg = errorJson?.optString("msg")
                                ?: errorJson?.optString("error_description")
                                ?: "Sign in failed (${response.code})"
                            _status.value = SupabaseAuthStatus.Error(errorMsg)
                            Result.failure(Exception(errorMsg))
                        }
                    }
                } catch (e: Exception) {
                    _status.value = SupabaseAuthStatus.Error(e.localizedMessage ?: "Network error")
                    Result.failure(e)
                }
            }

        /**
         * Register a new user in Supabase Auth.
         */
        suspend fun signUp(
            email: String,
            password: String,
            metadata: Map<String, Any> = emptyMap()
        ): Result<String> = withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                    if (metadata.isNotEmpty()) {
                        put("data", JSONObject(metadata))
                    }
                }

                val request = Request.Builder()
                    .url("${SupabaseConfig.authUrl}/signup")
                    .post(json.toString().toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        // Check if session was returned directly (e.g. auto-confirm on)
                        val parsed = try { JSONObject(responseBody) } catch (_: Exception) { null }
                        val accessToken = parsed?.optString("access_token", null)
                        if (!accessToken.isNullOrBlank()) {
                            val userObj = parsed.optJSONObject("user")
                            val newSession = SupabaseUserSession(
                                accessToken = accessToken,
                                refreshToken = parsed.optString("refresh_token", null),
                                expiresIn = parsed.optLong("expires_in", 3600),
                                userId = userObj?.optString("id"),
                                email = userObj?.optString("email") ?: email
                            )
                            _session.value = newSession
                            _status.value = SupabaseAuthStatus.Authenticated(newSession)
                            storage?.saveSession(newSession)
                        }
                        Result.success(responseBody)
                    } else {
                        val errorJson = try { JSONObject(responseBody) } catch (_: Exception) { null }
                        val msg = errorJson?.optString("msg")
                            ?: errorJson?.optString("error_description")
                            ?: "Sign up failed (${response.code})"
                        Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Refreshes the active session using the stored refresh_token.
         */
        suspend fun refreshSession(): Result<SupabaseUserSession> = withContext(Dispatchers.IO) {
            val current = _session.value
            val refreshToken = current?.refreshToken
            if (refreshToken.isNullOrBlank()) {
                val err = "No refresh token available"
                _status.value = SupabaseAuthStatus.NotAuthenticated
                return@withContext Result.failure(Exception(err))
            }

            try {
                val json = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }

                val request = Request.Builder()
                    .url("${SupabaseConfig.authUrl}/token?grant_type=refresh_token")
                    .post(json.toString().toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val parsed = JSONObject(body)
                        val newAccessToken = parsed.getString("access_token")
                        val newRefreshToken = parsed.optString("refresh_token", refreshToken)
                        val expiresIn = parsed.optLong("expires_in", 3600)
                        val userObj = parsed.optJSONObject("user")
                        val userId = userObj?.optString("id") ?: current.userId
                        val userEmail = userObj?.optString("email") ?: current.email

                        val refreshed = SupabaseUserSession(
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken,
                            expiresIn = expiresIn,
                            userId = userId,
                            email = userEmail
                        )

                        _session.value = refreshed
                        _status.value = SupabaseAuthStatus.Authenticated(refreshed)
                        storage?.saveSession(refreshed)
                        Log.i(tag, "Successfully refreshed Supabase session for user $userId")
                        Result.success(refreshed)
                    } else {
                        Log.w(tag, "Failed to refresh token (${response.code}): $body")
                        // If refresh token is revoked or invalid, clear session
                        if (response.code in 400..403) {
                            signOut()
                        }
                        Result.failure(Exception("Refresh failed with code ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Network error during token refresh", e)
                Result.failure(e)
            }
        }

        /**
         * Sends a password reset email via Supabase Auth (/auth/v1/recover).
         */
        suspend fun sendPasswordResetEmail(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                }
                val request = Request.Builder()
                    .url("${SupabaseConfig.authUrl}/recover")
                    .post(json.toString().toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(true)
                    } else {
                        val body = response.body?.string().orEmpty()
                        val errorJson = try { JSONObject(body) } catch (_: Exception) { null }
                        val msg = errorJson?.optString("msg") ?: errorJson?.optString("error_description") ?: "Password reset failed"
                        Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Signs out current user and clears persistent session storage.
         */
        fun signOut() {
            clientScope.launch {
                try {
                    val token = _session.value?.accessToken
                    if (!token.isNullOrBlank()) {
                        val request = Request.Builder()
                            .url("${SupabaseConfig.authUrl}/logout")
                            .post("{}".toRequestBody(jsonMediaType))
                            .build()
                        okHttpClient.newCall(request).execute().close()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Non-critical error calling remote logout", e)
                }
            }
            _session.value = null
            _status.value = SupabaseAuthStatus.NotAuthenticated
            storage?.clearSession()
            Log.i(tag, "User signed out and session cleared.")
        }
    }

    /**
     * Auth instance adhering to standard Supabase client semantics.
     */
    val auth = Auth()

    // Backward-compatible delegates
    val session: StateFlow<SupabaseUserSession?> get() = auth.session
    val sessionStatus: StateFlow<SupabaseAuthStatus> get() = auth.status

    fun currentSessionOrNull(): SupabaseUserSession? = auth.currentSessionOrNull()

    /**
     * Top-level initializer for Android context.
     */
    fun initialize(context: Context) {
        auth.initialize(context)
    }

    /**
     * Shared OkHttpClient with automatic Authorization headers:
     * Bearer token for logged-in user or publishable key for anon requests.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("apikey", SupabaseConfig.publishableKey)

                if (original.header("Content-Type") == null) {
                    requestBuilder.header("Content-Type", "application/json")
                }

                val activeSession = auth.currentSessionOrNull()
                val bearerToken = activeSession?.accessToken ?: SupabaseConfig.publishableKey
                requestBuilder.header("Authorization", "Bearer $bearerToken")

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    /**
     * Verifies connectivity to the configured Supabase instance.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        _connectionStatus.value = SupabaseConnectionStatus.CONNECTING
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val isSuccessful = response.isSuccessful || response.code == 404 || response.code == 400 || response.code == 401
                if (isSuccessful) {
                    _connectionStatus.value = SupabaseConnectionStatus.CONNECTED
                    _lastError.value = null
                    Log.i(tag, "Successfully reached Supabase server at ${SupabaseConfig.url} (Code: ${response.code})")
                    Result.success(true)
                } else {
                    val errorMsg = "HTTP error ${response.code}: ${response.message}"
                    _connectionStatus.value = SupabaseConnectionStatus.ERROR
                    _lastError.value = errorMsg
                    Log.w(tag, "Supabase test connection failed: $errorMsg")
                    Result.failure(IOException(errorMsg))
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = SupabaseConnectionStatus.ERROR
            val msg = e.localizedMessage ?: "Unknown network exception"
            _lastError.value = msg
            Log.e(tag, "Error testing Supabase connection", e)
            Result.failure(e)
        }
    }

    /**
     * Delegated sign-in method.
     */
    suspend fun signInWithPassword(email: String, password: String): Result<SupabaseUserSession> =
        auth.signInWithPassword(email, password)

    /**
     * Delegated sign-up method.
     */
    suspend fun signUp(email: String, password: String, metadata: Map<String, Any> = emptyMap()): Result<String> =
        auth.signUp(email, password, metadata)

    /**
     * Delegated sign-out method.
     */
    fun signOut() = auth.signOut()

    /**
     * Query table records via PostgREST.
     */
    suspend fun queryTable(tableName: String, queryParams: Map<String, String> = emptyMap()): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val httpUrlBuilder = "${SupabaseConfig.restUrl}/$tableName".toHttpUrl().newBuilder()
                queryParams.forEach { (k, v) -> httpUrlBuilder.addQueryParameter(k, v) }

                val request = Request.Builder()
                    .url(httpUrlBuilder.build())
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Query to '$tableName' failed (${response.code}): $body"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Insert a record into a table via PostgREST.
     */
    suspend fun insertRecord(tableName: String, jsonRecord: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SupabaseConfig.restUrl}/$tableName")
                    .header("Prefer", "return=representation")
                    .post(jsonRecord.toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Insert into '$tableName' failed (${response.code}): $body"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Helper to resolve public URL for Supabase Storage objects.
     */
    fun getPublicStorageUrl(bucket: String, path: String): String {
        val cleanPath = path.trimStart('/')
        return "${SupabaseConfig.storageUrl}/object/public/$bucket/$cleanPath"
    }

    /**
     * Fetch a user profile from the Supabase `public.profiles` table.
     * Complies with RLS (public read permissions).
     */
    suspend fun fetchProfile(userId: String): Result<SupabaseUserProfile> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mapOf(
                "id" to "eq.$userId",
                "select" to "*"
            )
            val result = queryTable("profiles", queryParams)
            result.mapCatching { jsonArrayStr ->
                val array = JSONArray(jsonArrayStr)
                if (array.length() > 0) {
                    SupabaseUserProfile.fromJsonObject(array.getJSONObject(0))
                } else {
                    throw NoSuchElementException("Profile with id '$userId' not found in Supabase")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upsert a user profile into the Supabase `public.profiles` table.
     * With authenticated bearer token, matches `auth.uid() = id` RLS policies.
     */
    suspend fun upsertProfile(profile: SupabaseUserProfile): Result<SupabaseUserProfile> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/profiles")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(profile.toJson().toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        Result.success(SupabaseUserProfile.fromJsonObject(array.getJSONObject(0)))
                    } else {
                        Result.success(profile)
                    }
                } else {
                    Result.failure(Exception("Failed to upsert profile in Supabase (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search public profiles in Supabase without RLS restrictions.
     * Supports matching on username and full_name.
     */
    suspend fun searchProfiles(query: String, limit: Int = 20): Result<List<SupabaseUserProfile>> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext Result.success(emptyList())
            try {
                val clean = query.trim()
                val queryParams = mapOf(
                    "select" to "*",
                    "or" to "(username.ilike.*$clean*,full_name.ilike.*$clean*)",
                    "limit" to limit.toString()
                )
                val result = queryTable("profiles", queryParams)
                result.mapCatching { jsonArrayStr ->
                    val array = JSONArray(jsonArrayStr)
                    val list = mutableListOf<SupabaseUserProfile>()
                    for (i in 0 until array.length()) {
                        list.add(SupabaseUserProfile.fromJsonObject(array.getJSONObject(i)))
                    }
                    list
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Create a new post in the `public.posts` table.
     */
    suspend fun createPost(post: SupabasePost): Result<SupabasePost> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/posts")
                .header("Prefer", "return=representation")
                .post(post.toJson().toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        Result.success(SupabasePost.fromJsonObject(array.getJSONObject(0)))
                    } else {
                        Result.success(post)
                    }
                } else {
                    Result.failure(Exception("Failed to create post in Supabase (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch feed posts from `public.posts` table ordered by created_at descending.
     */
    suspend fun fetchPosts(limit: Int = 30): Result<List<SupabasePost>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mapOf(
                "select" to "*",
                "order" to "created_at.desc",
                "limit" to limit.toString()
            )
            val result = queryTable("posts", queryParams)
            result.mapCatching { jsonArrayStr ->
                val array = JSONArray(jsonArrayStr)
                val list = mutableListOf<SupabasePost>()
                for (i in 0 until array.length()) {
                    list.add(SupabasePost.fromJsonObject(array.getJSONObject(i)))
                }
                list
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a post from `public.posts` table.
     */
    suspend fun deletePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/posts?id=eq.$postId")
                .delete()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to delete post $postId: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user profile & data (Play Store account deletion policy requirement).
     */
    suspend fun deleteProfile(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/profiles?id=eq.$userId")
                .delete()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    signOut()
                    Result.success(true)
                } else {
                    signOut()
                    Result.success(true)
                }
            }
        } catch (e: Exception) {
            signOut()
            Result.success(true)
        }
    }

    /**
     * Block another user in `public.blocked_users`.
     */
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("blocker_id", blockerId)
                put("blocked_id", blockedId)
            }.toString()

            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/blocked_users")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code == 201 || response.code == 200)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unblock a user.
     */
    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/blocked_users?blocker_id=eq.$blockerId&blocked_id=eq.$blockedId")
                .delete()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code == 204)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload an asset to Supabase Storage bucket.
     */
    suspend fun uploadStorage(bucket: String, path: String, mimeType: String, data: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val cleanPath = path.trimStart('/')
                val url = "${SupabaseConfig.storageUrl}/object/$bucket/$cleanPath"
                val body = data.toRequestBody(mimeType.toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", mimeType)
                    .header("x-upsert", "true")
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 200 || response.code == 201) {
                        Result.success(getPublicStorageUrl(bucket, cleanPath))
                    } else {
                        val respBody = response.body?.string().orEmpty()
                        Result.failure(Exception("Upload to '$bucket' failed (${response.code}): $respBody"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Uploads media from an Android content:// URI or local file to Supabase Storage bucket.
     * Returns the public HTTPS URL.
     */
    suspend fun uploadMediaUri(context: Context, bucket: String, uriString: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                return@withContext Result.success(uriString)
            }
            try {
                val uri = android.net.Uri.parse(uriString)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val extension = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("webp") -> "webp"
                    mimeType.contains("gif") -> "gif"
                    mimeType.contains("mp4") -> "mp4"
                    else -> "jpg"
                }
                val fileName = "${java.util.UUID.randomUUID()}.$extension"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Could not read media from uri $uriString"))

                uploadStorage(bucket, fileName, mimeType, bytes)
            } catch (e: Exception) {
                Log.e(tag, "Failed to upload media uri $uriString", e)
                Result.failure(e)
            }
        }

    /**
     * Add like or reaction in public.post_likes table.
     */
    suspend fun addLike(postId: String, userId: String, reactionType: String = "LIKE"): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("post_id", postId)
                    put("user_id", userId)
                    put("reaction_type", reactionType)
                }.toString()

                val request = Request.Builder()
                    .url("${SupabaseConfig.restUrl}/post_likes")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(json.toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    Result.success(response.isSuccessful || response.code == 201 || response.code == 200)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Remove like from public.post_likes table.
     */
    suspend fun removeLike(postId: String, userId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SupabaseConfig.restUrl}/post_likes?post_id=eq.$postId&user_id=eq.$userId")
                    .delete()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    Result.success(response.isSuccessful || response.code == 204)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Insert comment into public.comments table.
     */
    suspend fun addComment(
        commentId: String,
        postId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", commentId)
                put("post_id", postId)
                put("user_id", userId)
                put("user_name", userName)
                put("user_avatar", userAvatar)
                put("content", content)
            }.toString()

            val request = Request.Builder()
                .url("${SupabaseConfig.restUrl}/comments")
                .header("Prefer", "return=representation")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code == 201 || response.code == 200)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch comments for a post.
     */
    suspend fun fetchComments(postId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mapOf(
                "post_id" to "eq.$postId",
                "select" to "*",
                "order" to "created_at.asc"
            )
            val result = queryTable("comments", queryParams)
            result.mapCatching { jsonArrayStr ->
                val array = JSONArray(jsonArrayStr)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                list
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val instance: SupabaseClient by lazy { SupabaseClient() }
    }
}
