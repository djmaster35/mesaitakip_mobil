package com.example.mesaitakip.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kullanicilar")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val adsoyad: String,
    val is_admin: Int = 0,
    val is_banned: Int = 0
)
