package com.example.mesaitakip.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "mesai_kayitlari",
    foreignKeys = [
        ForeignKey(
            entity = Week::class,
            parentColumns = ["id"],
            childColumns = ["hafta_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OvertimeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hafta_id: Int,
    val tarih: String,
    val aciklama: String,
    val saat: String,
    val is_resmi_tatil: Int = 0
)
