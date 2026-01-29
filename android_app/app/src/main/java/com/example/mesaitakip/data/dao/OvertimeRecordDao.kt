package com.example.mesaitakip.data.dao

import androidx.room.*
import com.example.mesaitakip.data.entities.OvertimeRecord

@Dao
interface OvertimeRecordDao {
    @Query("SELECT * FROM mesai_kayitlari WHERE hafta_id = :weekId ORDER BY tarih ASC")
    suspend fun getRecordsForWeek(weekId: Int): List<OvertimeRecord>

    @Insert
    suspend fun insertRecord(record: OvertimeRecord): Long

    @Insert
    suspend fun insertRecords(records: List<OvertimeRecord>)

    @Update
    suspend fun updateRecord(record: OvertimeRecord)

    @Delete
    suspend fun deleteRecord(record: OvertimeRecord)

    @Query("DELETE FROM mesai_kayitlari WHERE hafta_id = :weekId")
    suspend fun deleteRecordsForWeek(weekId: Int)
}
