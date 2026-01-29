package com.example.mesaitakip.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mesaitakip.data.dao.OvertimeRecordDao
import com.example.mesaitakip.data.dao.UserDao
import com.example.mesaitakip.data.dao.WeekDao
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.data.entities.User
import com.example.mesaitakip.data.entities.Week
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Week::class, OvertimeRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun weekDao(): WeekDao
    abstract fun overtimeRecordDao(): OvertimeRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mesaitakip_room.db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.userDao())
                }
            }
        }

        suspend fun populateDatabase(userDao: UserDao) {
            // Add admin user if not exists
            if (userDao.getUserCount() == 0) {
                userDao.insertUser(User(username = "admin", password = "admin", adsoyad = "Sistem Yöneticisi", is_admin = 1))
            }
        }
    }
}
