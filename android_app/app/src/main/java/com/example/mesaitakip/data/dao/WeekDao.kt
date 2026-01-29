package com.example.mesaitakip.data.dao

import androidx.room.*
import com.example.mesaitakip.data.entities.Week

@Dao
interface WeekDao {
    @Query("SELECT * FROM haftalar WHERE user_id = :userId ORDER BY hafta_baslangic DESC")
    suspend fun getWeeksForUser(userId: Int): List<Week>

    @Query("SELECT * FROM haftalar WHERE id = :id")
    suspend fun getWeekById(id: Int): Week?

    @Insert
    suspend fun insertWeek(week: Week): Long

    @Delete
    suspend fun deleteWeek(week: Week)
}
