package com.example.data.firebase

import android.content.Context
import android.util.Log
import android.util.Patterns
import com.example.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service managing Firebase Authentication and Firestore backend user persistence.
 * Validates unique emails and usernames, manages secure session tokens,
 * and maintains synchronized profile records in Cloud Firestore.
 */
class FirebaseAuthService private constructor(private val appContext: Context) {

    private val tag = "FirebaseAuthService"

    init {
        ensureFirebaseInitialized(appContext)
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private fun ensureFirebaseInitialized(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val initialized = FirebaseApp.initializeApp(context)
                if (initialized == null) {
                    // Fallback to programmatic options if resources are unavailable
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:542947351052:android:7f4c9a8123bc68ef01d2ab")
                        .setProjectId("fadx-social-app")
                        .setApiKey("AIzaSyDummyFadxClientKey1234567890abcdef")
                        .setStorageBucket("fadx-social-app.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.d(tag, "FirebaseApp initialized with fallback options")
                } else {
                    Log.d(tag, "FirebaseApp initialized successfully")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "FirebaseApp initialization handled: ${e.message}")
        }
    }

    /**
     * Checks if an email is already registered in Firebase or Firestore.
     */
    suspend fun isEmailAvailable(email: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return@withContext false
        }
        try {
            // Check Firestore users collection for existing email
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", trimmed)
                .limit(1)
                .get()
                .awaitResult()

            return@withContext snapshot.isEmpty
        } catch (e: Exception) {
            Log.w(tag, "Failed to query Firestore for email availability: ${e.message}")
            // Assume available to allow Firebase Auth's atomic uniqueness check
            true
        }
    }

    /**
     * Checks if a username is already taken.
     */
    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = username.trim().lowercase()
        if (trimmed.length < 3) return@withContext false
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", trimmed)
                .limit(1)
                .get()
                .awaitResult()

            return@withContext snapshot.isEmpty
        } catch (e: Exception) {
            Log.w(tag, "Failed to query Firestore for username availability: ${e.message}")
            true
        }
    }

    /**
     * Creates a new Firebase Auth account and persists user profile in Firestore.
     * Enforces unique emails and minimum password standards.
     */
    suspend fun signUp(
        name: String,
        username: String,
        email: String,
        phone: String,
        pass: String,
        dob: String,
        gender: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().lowercase()

        // 1. Format Validation
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }

        try {
            // 2. Pre-check username in Firestore
            val isUsernameUnique = isUsernameAvailable(cleanUsername)
            if (!isUsernameUnique) {
                return@withContext Result.failure(IllegalStateException("The username '@$cleanUsername' is already taken."))
            }

            // 3. Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).awaitResult()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Firebase user creation failed."))

            val uid = firebaseUser.uid

            // 4. Update Firebase Auth display name
            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }
                firebaseUser.updateProfile(profileUpdates).awaitResult()
            } catch (e: Exception) {
                Log.w(tag, "Could not update user display name: ${e.message}")
            }

            // 5. Construct user model
            val newUser = User(
                id = uid,
                name = name.ifBlank { cleanUsername },
                username = cleanUsername,
                email = cleanEmail,
                phone = phone.ifBlank { "+1 555 019 2834" },
                bio = "Hey there! I am using Fadx.",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
                location = "San Francisco, CA",
                website = "https://fadx.social",
                followersCount = 0,
                followingCount = 0,
                friendsCount = 0,
                isVerified = false,
                isOnline = true,
                lastActive = "Just now",
                gender = gender.ifBlank { "Not Specified" },
                dob = dob.ifBlank { "2000-01-01" }
            )

            // 6. Persist user data in Firestore backend
            persistUserToFirestore(newUser)

            Result.success(newUser)
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e(tag, "Email collision: ${e.message}")
            Result.failure(IllegalStateException("An account with this email address already exists. Please log in instead."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Log.e(tag, "Weak password: ${e.message}")
            Result.failure(IllegalArgumentException("The password provided is too weak. Please use at least 6 characters."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(tag, "Invalid credentials: ${e.message}")
            Result.failure(IllegalArgumentException("The email address is formatted incorrectly."))
        } catch (e: Exception) {
            Log.e(tag, "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in using Firebase Auth. Supports email or username identifier.
     */
    suspend fun signIn(identifier: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val trimmed = identifier.trim()
        if (trimmed.isBlank() || pass.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please provide credentials."))
        }

        try {
            // Resolve email if user entered a username
            val targetEmail = if (trimmed.contains("@")) {
                trimmed.lowercase()
            } else {
                val resolved = resolveEmailFromUsername(trimmed.lowercase())
                resolved ?: return@withContext Result.failure(
                    IllegalArgumentException("No account found with username '$trimmed'.")
                )
            }

            // Sign in to Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(targetEmail, pass).awaitResult()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Firebase sign in failed."))

            val uid = firebaseUser.uid

            // Load user profile from Firestore backend
            val user = loadUserFromFirestore(uid) ?: User(
                id = uid,
                name = firebaseUser.displayName ?: targetEmail.substringBefore("@").replace(".", " "),
                username = targetEmail.substringBefore("@").filter { it.isLetterOrDigit() }.lowercase(),
                email = targetEmail,
                phone = firebaseUser.phoneNumber ?: "+1 555 019 2834",
                avatarUrl = firebaseUser.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
                bio = "Welcome to my Fadx profile!",
                isOnline = true,
                lastActive = "Just now"
            )

            // Ensure profile exists in Firestore
            persistUserToFirestore(user)

            Result.success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e(tag, "Invalid user: ${e.message}")
            Result.failure(IllegalArgumentException("No account found with this email or username."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e(tag, "Invalid credentials: ${e.message}")
            Result.failure(IllegalArgumentException("Incorrect password or credentials."))
        } catch (e: Exception) {
            Log.e(tag, "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves an email address from a username by querying the Firestore backend.
     */
    private suspend fun resolveEmailFromUsername(username: String): String? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .awaitResult()

            if (!snapshot.isEmpty) {
                snapshot.documents.firstOrNull()?.getString("email")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Error resolving username in Firestore: ${e.message}")
            null
        }
    }

    /**
     * Persists user details in the Firestore `users/{uid}` collection.
     */
    suspend fun persistUserToFirestore(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val map = hashMapOf(
                "uid" to user.id,
                "name" to user.name,
                "username" to user.username.lowercase(),
                "email" to user.email.lowercase(),
                "phone" to user.phone,
                "bio" to user.bio,
                "avatarUrl" to user.avatarUrl,
                "coverUrl" to user.coverUrl,
                "location" to user.location,
                "website" to user.website,
                "followersCount" to user.followersCount,
                "followingCount" to user.followingCount,
                "friendsCount" to user.friendsCount,
                "isVerified" to user.isVerified,
                "gender" to user.gender,
                "dob" to user.dob,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.id)
                .set(map, SetOptions.merge())
                .awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist user profile to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Loads a user profile from Firestore by UID.
     */
    suspend fun loadUserFromFirestore(uid: String): User? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(uid).get().awaitResult()
            if (doc.exists()) {
                User(
                    id = uid,
                    name = doc.getString("name") ?: "User",
                    username = doc.getString("username") ?: uid.take(8),
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "+1 555 019 2834",
                    bio = doc.getString("bio") ?: "",
                    avatarUrl = doc.getString("avatarUrl") ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&q=80",
                    coverUrl = doc.getString("coverUrl") ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80",
                    location = doc.getString("location") ?: "San Francisco, CA",
                    website = doc.getString("website") ?: "https://fadx.social",
                    followersCount = doc.getLong("followersCount")?.toInt() ?: 0,
                    followingCount = doc.getLong("followingCount")?.toInt() ?: 0,
                    friendsCount = doc.getLong("friendsCount")?.toInt() ?: 0,
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    isOnline = true,
                    lastActive = "Just now",
                    gender = doc.getString("gender") ?: "Not Specified",
                    dob = doc.getString("dob") ?: "2000-01-01"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load user from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Sends a password reset email via Firebase Auth.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        try {
            auth.sendPasswordResetEmail(cleanEmail).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Password reset error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Signs out of Firebase Auth.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(tag, "Error signing out of Firebase: ${e.message}")
        }
    }

    /**
     * Gets the currently authenticated Firebase User if an active session exists.
     */
    fun getFirebaseUser(): FirebaseUser? {
        return try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseAuthService? = null

        fun getInstance(context: Context): FirebaseAuthService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Extension function to safely convert a Google Play Services Task to a suspending coroutine
 * without requiring external play-services-coroutines dependencies.
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) {
            cont.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (cont.isActive) {
            cont.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (cont.isActive) {
            cont.cancel()
        }
    }
}
