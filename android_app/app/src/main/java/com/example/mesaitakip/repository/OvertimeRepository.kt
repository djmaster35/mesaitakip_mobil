package com.example.mesaitakip.repository

import com.example.mesaitakip.data.dao.OvertimeRecordDao
import com.example.mesaitakip.data.dao.WeekDao
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.data.entities.Week

class OvertimeRepository(
    private val weekDao: WeekDao,
    private val recordDao: OvertimeRecordDao
) {
    suspend fun getWeeksForUser(userId: Int): List<Week> = weekDao.getWeeksForUser(userId)
    suspend fun getWeekById(id: Int): Week? = weekDao.getWeekById(id)
    suspend fun insertWeek(week: Week): Long = weekDao.insertWeek(week)
    suspend fun deleteWeek(week: Week) = weekDao.deleteWeek(week)

    suspend fun getRecordsForWeek(weekId: Int): List<OvertimeRecord> = recordDao.getRecordsForWeek(weekId)
    suspend fun insertRecord(record: OvertimeRecord): Long = recordDao.insertRecord(record)
    suspend fun insertRecords(records: List<OvertimeRecord>) = recordDao.insertRecords(records)
    suspend fun updateRecord(record: OvertimeRecord) = recordDao.updateRecord(record)
    suspend fun deleteRecord(record: OvertimeRecord) = recordDao.deleteRecord(record)
    suspend fun deleteRecordsForWeek(weekId: Int) = recordDao.deleteRecordsForWeek(weekId)
}
