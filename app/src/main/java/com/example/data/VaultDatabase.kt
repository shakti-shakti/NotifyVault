package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NotificationEntity::class,
        AppInfoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appInfoDao(): AppInfoDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                // Purge any legacy database containing previous mock data
                try {
                    context.applicationContext.deleteDatabase("notifyvault.db")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "notifyvault_live_v3.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
