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
        AppInfoEntity::class,
        NotificationHistoryEntity::class
    ],
    version = 5,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN fingerprintKey TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notifications ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notifications ADD COLUMN lastSeenAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notifications ADD COLUMN updateCount INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE notifications ADD COLUMN isUpdate INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_fingerprintKey ON notifications(fingerprintKey)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_contentHash ON notifications(contentHash)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_lastSeenAt ON notifications(lastSeenAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_fingerprintKey_lastSeenAt ON notifications(fingerprintKey, lastSeenAt)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        parentId INTEGER NOT NULL,
                        previousTitle TEXT,
                        previousText TEXT,
                        previousContentHash TEXT NOT NULL,
                        replacedAt INTEGER NOT NULL,
                        FOREIGN KEY(parentId) REFERENCES notifications(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notification_history_parentId ON notification_history(parentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notification_history_replacedAt ON notification_history(replacedAt)")
                database.execSQL("UPDATE notifications SET lastSeenAt = captureTime WHERE lastSeenAt = 0")
                database.execSQL("UPDATE notifications SET fingerprintKey = notificationKey WHERE fingerprintKey = ''")
            }
        }
    }
}
