package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SupabaseUserSession(
    val accessToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long = 3600,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null
)

enum class SupabaseConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR
}

class SupabaseClient private constructor() {

    private val tag = "SupabaseClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _session = MutableStateFlow<SupabaseUserSession?>(null)
    val session: StateFlow<SupabaseUserSession?> = _session.asStateFlow()

    private val _connectionStatus = MutableStateFlow(SupabaseConnectionStatus.IDLE)
    val connectionStatus: StateFlow<SupabaseConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

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
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")

                val currentToken = _session.value?.accessToken ?: SupabaseConfig.publishableKey
                requestBuilder.header("Authorization", "Bearer $currentToken")

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
                // Supabase OpenAPI schema or root 200/404/204 response proves endpoint reachability
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
     * Sign in a user with email and password via Supabase Auth.
     */
    suspend fun signInWithPassword(email: String, password: String): Result<SupabaseUserSession> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email)
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
                        val userEmail = userObj?.optString("email")

                        val newSession = SupabaseUserSession(
                            accessToken = accessToken,
                            tokenType = tokenType,
                            expiresIn = expiresIn,
                            refreshToken = refreshToken,
                            userId = userId,
                            email = userEmail
                        )
                        _session.value = newSession
                        _connectionStatus.value = SupabaseConnectionStatus.CONNECTED
                        Result.success(newSession)
                    } else {
                        val errorJson = try { JSONObject(responseBody) } catch (_: Exception) { null }
                        val errorMsg = errorJson?.optString("msg")
                            ?: errorJson?.optString("error_description")
                            ?: "Sign in failed with status ${response.code}"
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sign up a new user via Supabase Auth.
     */
    suspend fun signUp(email: String, password: String, metadata: Map<String, Any> = emptyMap()): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email)
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
                        Result.success(responseBody)
                    } else {
                        val errorJson = try { JSONObject(responseBody) } catch (_: Exception) { null }
                        val msg = errorJson?.optString("msg") ?: "Sign up failed (${response.code})"
                        Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Clears current authenticated session.
     */
    fun signOut() {
        _session.value = null
    }

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

    companion object {
        val instance: SupabaseClient by lazy { SupabaseClient() }
    }
}
