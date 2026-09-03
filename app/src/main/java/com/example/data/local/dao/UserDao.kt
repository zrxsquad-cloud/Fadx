package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local caching of user profiles.
 * Adheres to Room best practices: Flow queries for reactive streams,
 * suspend functions for mutations, and conflict resolution.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM user_profiles ORDER BY cached_at DESC")
    fun getAllCachedUsers(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdDirect(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserProfileEntity>)

    @Update
    suspend fun updateUser(user: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getUserCount(): Int
}
