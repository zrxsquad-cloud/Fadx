package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.supabase.SupabaseUserProfile
import com.example.model.User

/**
 * Room Entity representing a cached User Profile stored locally in SQLite.
 * Provides offline-first access, immediate UI rendering, and bidirectional mapping
 * between Supabase schema and local presentation models.
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "username")
    val username: String = "",

    @ColumnInfo(name = "full_name")
    val fullName: String = "",

    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "phone")
    val phone: String = "",

    @ColumnInfo(name = "bio")
    val bio: String = "",

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String = "",

    @ColumnInfo(name = "cover_url")
    val coverUrl: String = "",

    @ColumnInfo(name = "location")
    val location: String = "",

    @ColumnInfo(name = "website")
    val website: String = "",

    @ColumnInfo(name = "followers_count")
    val followersCount: Int = 0,

    @ColumnInfo(name = "following_count")
    val followingCount: Int = 0,

    @ColumnInfo(name = "friends_count")
    val friendsCount: Int = 0,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false,

    @ColumnInfo(name = "last_active")
    val lastActive: String = "Recently",

    @ColumnInfo(name = "gender")
    val gender: String = "Not Specified",

    @ColumnInfo(name = "dob")
    val dob: String = "",

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this local Room entity into a SupabaseUserProfile representation.
     */
    fun toSupabaseProfile(): SupabaseUserProfile {
        return SupabaseUserProfile(
            id = id,
            email = email.ifBlank { null },
            username = username.ifBlank { null },
            fullName = fullName.ifBlank { null },
            avatarUrl = avatarUrl.ifBlank { null },
            coverUrl = coverUrl.ifBlank { null },
            bio = bio.ifBlank { null },
            website = website.ifBlank { null },
            phone = phone.ifBlank { null },
            location = location.ifBlank { null },
            followersCount = followersCount,
            followingCount = followingCount,
            friendsCount = friendsCount,
            isVerified = isVerified,
            isOnline = isOnline,
            lastActive = lastActive,
            gender = gender,
            dob = dob
        )
    }

    /**
     * Converts this local Room entity into the UI User model.
     */
    fun toAppUser(): User {
        return User(
            id = id,
            name = fullName.ifBlank { username.ifBlank { "User" } },
            username = username.ifBlank { id.take(8) },
            email = email,
            phone = phone,
            bio = bio,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            location = location,
            website = website,
            followersCount = followersCount,
            followingCount = followingCount,
            friendsCount = friendsCount,
            isVerified = isVerified,
            isOnline = isOnline,
            lastActive = lastActive,
            gender = gender,
            dob = dob
        )
    }

    companion object {
        /**
         * Creates a Room entity from a SupabaseUserProfile.
         */
        fun fromSupabaseProfile(
            profile: SupabaseUserProfile,
            cachedAt: Long = System.currentTimeMillis()
        ): UserProfileEntity {
            return UserProfileEntity(
                id = profile.id,
                username = profile.username.orEmpty(),
                fullName = profile.fullName.orEmpty(),
                email = profile.email.orEmpty(),
                phone = profile.phone.orEmpty(),
                bio = profile.bio.orEmpty(),
                avatarUrl = profile.avatarUrl.orEmpty(),
                coverUrl = profile.coverUrl.orEmpty(),
                location = profile.location.orEmpty(),
                website = profile.website.orEmpty(),
                followersCount = profile.followersCount,
                followingCount = profile.followingCount,
                friendsCount = profile.friendsCount,
                isVerified = profile.isVerified,
                isOnline = profile.isOnline,
                lastActive = profile.lastActive ?: "Recently",
                gender = profile.gender ?: "Not Specified",
                dob = profile.dob.orEmpty(),
                cachedAt = cachedAt,
                updatedAt = System.currentTimeMillis()
            )
        }

        /**
         * Creates a Room entity from the UI User model.
         */
        fun fromAppUser(
            user: User,
            cachedAt: Long = System.currentTimeMillis()
        ): UserProfileEntity {
            return UserProfileEntity(
                id = user.id,
                username = user.username,
                fullName = user.name,
                email = user.email,
                phone = user.phone,
                bio = user.bio,
                avatarUrl = user.avatarUrl,
                coverUrl = user.coverUrl,
                location = user.location,
                website = user.website,
                followersCount = user.followersCount,
                followingCount = user.followingCount,
                friendsCount = user.friendsCount,
                isVerified = user.isVerified,
                isOnline = user.isOnline,
                lastActive = user.lastActive,
                gender = user.gender,
                dob = user.dob,
                cachedAt = cachedAt,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
