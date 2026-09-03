package com.example.data.local

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserProfileEntity
import com.example.data.supabase.SupabaseUserProfile
import com.example.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository abstracting local SQLite cache operations via Room UserDao.
 */
class UserCacheRepository(private val userDao: UserDao) {

    /**
     * Reactive stream of all cached user profiles.
     */
    val allCachedUsers: Flow<List<UserProfileEntity>> = userDao.getAllCachedUsers()

    /**
     * Observe a specific user profile by ID as Flow.
     */
    fun getUser(userId: String): Flow<UserProfileEntity?> = userDao.getUserById(userId)

    /**
     * Direct one-shot retrieval of a cached user profile.
     */
    suspend fun getUserDirect(userId: String): UserProfileEntity? = userDao.getUserByIdDirect(userId)

    /**
     * Cache a Supabase profile.
     */
    suspend fun cacheSupabaseProfile(profile: SupabaseUserProfile) {
        userDao.insertUser(UserProfileEntity.fromSupabaseProfile(profile))
    }

    /**
     * Cache an app-level User model.
     */
    suspend fun cacheAppUser(user: User) {
        userDao.insertUser(UserProfileEntity.fromAppUser(user))
    }

    /**
     * Cache multiple users at once.
     */
    suspend fun cacheUsers(users: List<UserProfileEntity>) {
        userDao.insertUsers(users)
    }

    /**
     * Remove a user from the local cache.
     */
    suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    /**
     * Clear all cached users.
     */
    suspend fun clearCache() {
        userDao.clearAll()
    }
}
