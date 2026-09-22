package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.NotificationRepository
import com.example.data.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotifyVaultNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: VaultDatabase

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = VaultDatabase.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rescanActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processStatusBarNotification(sbn)
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification) {
        val pkgName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Filter our own internal ongoing foreground notifications, but allow intentional test verifications
        val isTestNotification = extras.getBoolean("IS_NOTIFY_VAULT_TEST", false)
        if (pkgName == applicationContext.packageName && !isTestNotification) return

        val pm: PackageManager = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

        // Extract action titles
        val actionTitles = mutableListOf<String>()
        notification.actions?.forEach { action ->
            action.title?.let { actionTitles.add(it.toString()) }
        }

        // Convert key extras to JSON
        val extrasJson = JSONObject().apply {
            extras.keySet()?.forEach { key ->
                val value = extras.get(key)
                if (value != null && value !is ByteArray) {
                    put(key, value.toString().take(200))
                }
            }
        }.toString()

        val parsedEntity = NotificationRepository.parseNotification(
            packageName = pkgName,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            category = notification.category ?: "SYSTEM",
            channelId = notification.channelId,
            key = sbn.key ?: "${pkgName}_${sbn.id}_${sbn.postTime}",
            extrasJson = extrasJson
        ).copy(
            postTime = sbn.postTime,
            isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
            isClearable = sbn.isClearable,
            actionLabels = actionTitles.joinToString(", ")
        )

        serviceScope.launch {
            try {
                database.notificationDao().insert(parsedEntity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Kept in vault archive forever per design
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NotifyVault Core Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live encrypted notification archive monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "notifyvault_live_capture"
        const val NOTIFICATION_ID = 9010
        private var instance: NotifyVaultNotificationListenerService? = null

        fun rescanActiveNotifications() {
            try {
                val service = instance ?: return
                val active = service.activeNotifications ?: return
                for (sbn in active) {
                    service.processStatusBarNotification(sbn)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isConnected(): Boolean = instance != null
    }
}
