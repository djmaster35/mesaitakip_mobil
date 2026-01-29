package com.example.mesaitakip

import android.app.Application
import com.example.mesaitakip.data.AppDatabase
import com.example.mesaitakip.repository.OvertimeRepository
import com.example.mesaitakip.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MesaiApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val userRepository by lazy { UserRepository(database.userDao()) }
    val overtimeRepository by lazy { OvertimeRepository(database.weekDao(), database.overtimeRecordDao()) }
}
