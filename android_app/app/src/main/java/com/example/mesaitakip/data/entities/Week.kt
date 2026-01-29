package com.example.mesaitakip.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "haftalar",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Week(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hafta_baslangic: String,
    val hafta_araligi: String,
    val calisan_adi: String,
    val user_id: Int
)
