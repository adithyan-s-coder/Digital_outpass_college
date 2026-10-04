package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.GateLogDao
import com.example.data.local.dao.OutpassDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entities.GateLogEntity
import com.example.data.local.entities.OutpassEntity
import com.example.data.local.entities.UserEntity

@Database(
    entities = [UserEntity::class, OutpassEntity::class, GateLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun outpassDao(): OutpassDao
    abstract fun gateLogDao(): GateLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vetias_outpass_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
