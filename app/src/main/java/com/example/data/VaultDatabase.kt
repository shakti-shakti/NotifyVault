package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NotificationEntity::class,
        AppInfoEntity::class
    ],
    version = 4,
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
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN deepLinkUri TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN deepLinkSource TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN deepLinkConfidence TEXT")
            }
        }
    }
}
