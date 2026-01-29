package com.example.mesaitakip.data.dao

import androidx.room.*
import com.example.mesaitakip.data.entities.User

@Dao
interface UserDao {
    @Query("SELECT * FROM kullanicilar WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM kullanicilar")
    suspend fun getAllUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT COUNT(*) FROM kullanicilar")
    suspend fun getUserCount(): Int
}
