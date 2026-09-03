package com.example.data.supabase

import com.example.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

/**
 * Data class representing the User Profile structure in Supabase PostgreSQL (e.g. `profiles` table).
 * Uses snake_case JSON keys corresponding to standard Supabase schemas with Row-Level Security (RLS).
 */
@JsonClass(generateAdapter = true)
data class SupabaseUserProfile(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "followers_count") val followersCount: Int = 0,
    @Json(name = "following_count") val followingCount: Int = 0,
    @Json(name = "friends_count") val friendsCount: Int = 0,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "is_online") val isOnline: Boolean = false,
    @Json(name = "last_active") val lastActive: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    /**
     * Converts to JSON string suitable for Supabase REST API requests.
     */
    fun toJson(): String = toJsonObject().toString()

    /**
     * Converts to JSONObject representation for PostgREST payloads.
     */
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            email?.let { put("email", it) }
            username?.let { put("username", it) }
            fullName?.let { put("full_name", it) }
            avatarUrl?.let { put("avatar_url", it) }
            coverUrl?.let { put("cover_url", it) }
            bio?.let { put("bio", it) }
            website?.let { put("website", it) }
            phone?.let { put("phone", it) }
            location?.let { put("location", it) }
            put("followers_count", followersCount)
            put("following_count", followingCount)
            put("friends_count", friendsCount)
            put("is_verified", isVerified)
            put("is_online", isOnline)
            lastActive?.let { put("last_active", it) }
            gender?.let { put("gender", it) }
            dob?.let { put("dob", it) }
            createdAt?.let { put("created_at", it) }
            updatedAt?.let { put("updated_at", it) }
        }
    }

    /**
     * Maps this Supabase model to the app-level User model for UI presentation.
     */
    fun toAppUser(): User {
        return User(
            id = id,
            name = fullName ?: username ?: "User",
            username = username ?: id.take(8),
            email = email ?: "",
            phone = phone ?: "",
            bio = bio ?: "",
            avatarUrl = avatarUrl ?: "",
            coverUrl = coverUrl ?: "",
            location = location ?: "",
            website = website ?: "",
            followersCount = followersCount,
            followingCount = followingCount,
            friendsCount = friendsCount,
            isVerified = isVerified,
            isOnline = isOnline,
            lastActive = lastActive ?: "Recently",
            gender = gender ?: "Not Specified",
            dob = dob ?: ""
        )
    }

    companion object {
        /**
         * Creates a SupabaseUserProfile from the app-level User model.
         */
        fun fromAppUser(user: User, email: String? = user.email): SupabaseUserProfile {
            return SupabaseUserProfile(
                id = user.id,
                email = email ?: user.email,
                username = user.username,
                fullName = user.name,
                avatarUrl = user.avatarUrl,
                coverUrl = user.coverUrl,
                bio = user.bio,
                website = user.website,
                phone = user.phone,
                location = user.location,
                followersCount = user.followersCount,
                followingCount = user.followingCount,
                friendsCount = user.friendsCount,
                isVerified = user.isVerified,
                isOnline = user.isOnline,
                lastActive = user.lastActive,
                gender = user.gender,
                dob = user.dob
            )
        }

        private fun JSONObject.optStringOrNull(key: String): String? {
            return if (has(key) && !isNull(key)) {
                val str = optString(key)
                if (str.isNotBlank() && str != "null") str else null
            } else null
        }

        /**
         * Deserializes a JSONObject received from Supabase REST response.
         */
        fun fromJsonObject(json: JSONObject): SupabaseUserProfile {
            val nameFallback = json.optStringOrNull("name")
            return SupabaseUserProfile(
                id = json.optString("id", ""),
                email = json.optStringOrNull("email"),
                username = json.optStringOrNull("username"),
                fullName = json.optStringOrNull("full_name") ?: nameFallback,
                avatarUrl = json.optStringOrNull("avatar_url"),
                coverUrl = json.optStringOrNull("cover_url"),
                bio = json.optStringOrNull("bio"),
                website = json.optStringOrNull("website"),
                phone = json.optStringOrNull("phone"),
                location = json.optStringOrNull("location"),
                followersCount = json.optInt("followers_count", 0),
                followingCount = json.optInt("following_count", 0),
                friendsCount = json.optInt("friends_count", 0),
                isVerified = json.optBoolean("is_verified", false),
                isOnline = json.optBoolean("is_online", false),
                lastActive = json.optStringOrNull("last_active"),
                gender = json.optStringOrNull("gender"),
                dob = json.optStringOrNull("dob"),
                createdAt = json.optStringOrNull("created_at"),
                updatedAt = json.optStringOrNull("updated_at")
            )
        }

        /**
         * Safely parses from JSON string.
         */
        fun fromJson(jsonStr: String): SupabaseUserProfile? {
            return try {
                fromJsonObject(JSONObject(jsonStr))
            } catch (e: Exception) {
                null
            }
        }
    }
}
