package com.example.mesaitakip.repository

import com.example.mesaitakip.data.dao.UserDao
import com.example.mesaitakip.data.entities.User

class UserRepository(private val userDao: UserDao) {
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)
    suspend fun getAllUsers(): List<User> = userDao.getAllUsers()
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
}
