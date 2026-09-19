package com.example.data.auth

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Stores registered academic user profiles and credentials locally on-device.
 */
@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccount(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val affiliation: String,
    val researchField: String,
    val avatarUri: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Dao
interface UserAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE isActive = 1 ORDER BY lastLoginAt DESC LIMIT 1")
    fun getActiveUser(): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE isActive = 1 ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getActiveUserOnce(): UserAccount?

    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("UPDATE user_accounts SET lastLoginAt = :timestamp, isActive = 1 WHERE LOWER(email) = LOWER(:email)")
    suspend fun recordLogin(email: String, timestamp: Long)

    @Query("UPDATE user_accounts SET isActive = 0 WHERE LOWER(email) = LOWER(:email)")
    suspend fun deactivateUser(email: String)

    @Query("UPDATE user_accounts SET isActive = 0")
    suspend fun deactivateAllUsers()

    @Query("DELETE FROM user_accounts WHERE LOWER(email) = LOWER(:email)")
    suspend fun deleteUser(email: String)

    @Query("DELETE FROM user_accounts")
    suspend fun clearAllUsers()

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun countUsers(): Int
}
