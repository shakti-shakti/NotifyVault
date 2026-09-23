package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.data.AppInfoResolver
import com.example.data.DeepLinkExtractor
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import com.example.data.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NotifyVaultNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: VaultDatabase
    private val liveRegistry = LiveNotificationRegistry.getInstance()

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = VaultDatabase.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onDestroy() {
        liveRegistry.clear()
        serviceScope.cancel()
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        rescanActiveNotifications()
    }

    override fun onListenerDisconnected() {
        liveRegistry.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processStatusBarNotification(sbn)
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification) {
        val pkgName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle()

        // Filter our own internal ongoing foreground notifications, but allow test alerts
        val isTestNotification = extras.getBoolean("IS_NOTIFY_VAULT_TEST", false)
        if (pkgName == applicationContext.packageName && !isTestNotification) return

        liveRegistry.put(LiveNotificationRegistry.fromStatusBarNotification(sbn))
        val extractedLink = DeepLinkExtractor.extract(notification, pkgName, applicationContext)

        // 1. Resolve human-readable App Name and App Icon
        val resolvedApp = AppInfoResolver.resolveSync(applicationContext, pkgName, database)
        val appName = resolvedApp.appName
        val appIconPath = resolvedApp.iconPath

        // 2. Text fields
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

        // 3. Bitmaps: Large Icon, Picture, Small Icon
        var largeIconPath: String? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.getLargeIcon()?.loadDrawable(applicationContext)?.let { drawable ->
                    val bmp = if (drawable is BitmapDrawable) drawable.bitmap else null
                    if (bmp != null) {
                        largeIconPath = AppInfoResolver.saveBitmapToFile(
                            applicationContext,
                            "notification_images",
                            "large_${pkgName}_${sbn.id}_${sbn.postTime}",
                            bmp
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val bmp = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
                if (bmp != null) {
                    largeIconPath = AppInfoResolver.saveBitmapToFile(
                        applicationContext,
                        "notification_images",
                        "large_${pkgName}_${sbn.id}_${sbn.postTime}",
                        bmp
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore icon extraction failure
        }

        var picturePath: String? = null
        try {
            @Suppress("DEPRECATION")
            val picBitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            if (picBitmap != null) {
                picturePath = AppInfoResolver.saveBitmapToFile(
                    applicationContext,
                    "notification_images",
                    "pic_${pkgName}_${sbn.id}_${sbn.postTime}",
                    picBitmap
                )
            }
        } catch (e: Exception) {
            // Ignore picture extraction failure
        }

        // 4. Channel Details & Importance
        var channelName: String? = null
        var importance = 3
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notification.channelId
            if (channelId != null) {
                val nm = getSystemService(NotificationManager::class.java)
                val channel = nm?.getNotificationChannel(channelId)
                channelName = channel?.name?.toString()
                importance = channel?.importance ?: 3
            }
        }

        // 5. Sound / Vibrate / Light flags
        val flags = notification.flags
        val hasSound = (notification.sound != null) || ((notification.defaults and Notification.DEFAULT_SOUND) != 0)
        val hasVibrate = (notification.vibrate != null) || ((notification.defaults and Notification.DEFAULT_VIBRATE) != 0)
        val hasLights = ((flags and Notification.FLAG_SHOW_LIGHTS) != 0) || ((notification.defaults and Notification.DEFAULT_LIGHTS) != 0)

        // 6. Action items
        val actionsArray = JSONArray()
        val actionTitles = mutableListOf<String>()
        notification.actions?.forEach { action ->
            val actionJson = JSONObject()
            val actionTitle = action.title?.toString() ?: ""
            actionJson.put("title", actionTitle)
            if (actionTitle.isNotBlank()) {
                actionTitles.add(actionTitle)
            }
            actionsArray.put(actionJson)
        }

        // 7. MessagingStyle messages
        val messagesArray = JSONArray()
        try {
            val messageBundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            messageBundles?.forEach { msgBundle ->
                if (msgBundle is Bundle) {
                    val msgJson = JSONObject()
                    msgJson.put("text", msgBundle.getCharSequence("text")?.toString() ?: "")
                    msgJson.put("time", msgBundle.getLong("time"))
                    val sender = msgBundle.getCharSequence("sender")?.toString()
                        ?: msgBundle.getBundle("sender_person")?.getCharSequence("name")?.toString()
                        ?: ""
                    msgJson.put("sender", sender)
                    messagesArray.put(msgJson)
                }
            }
        } catch (e: Exception) {
            // Ignore messaging extraction failure
        }

        // 8. People / Participants
        val peopleArray = JSONArray()
        try {
            val peopleList = extras.getStringArrayList(Notification.EXTRA_PEOPLE)
            peopleList?.forEach { peopleArray.put(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val peoplePersons = extras.getParcelableArrayList<android.app.Person>(Notification.EXTRA_PEOPLE_LIST)
                peoplePersons?.forEach { p ->
                    peopleArray.put(p.name?.toString() ?: p.uri ?: "")
                }
            }
        } catch (e: Exception) {
            // Ignore people extraction
        }

        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)

        // 9. Full extras bundle serialized to JSON
        val extrasJson = JSONObject().apply {
            extras.keySet()?.forEach { key ->
                val value = extras.get(key)
                if (value != null && value !is ByteArray) {
                    put(key, value.toString().take(250))
                }
            }
        }.toString()

        val isOngoingEvent = (flags and Notification.FLAG_ONGOING_EVENT) != 0

        // Check Additive Exclusion Rules before archiving
        val excludeRules = com.example.data.ExcludeRulesRepository.getInstance(applicationContext)
        val excludeReason = excludeRules.shouldExclude(
            packageName = pkgName,
            appName = appName,
            title = title ?: "",
            text = text ?: "",
            bigText = bigText ?: "",
            category = notification.category ?: "SYSTEM",
            isOngoing = isOngoingEvent,
            priority = notification.priority
        )
        if (excludeReason != null) {
            // Notification excluded by capture rules — do not persist
            return
        }

        val parsedEntity = NotificationRepository.parseNotification(
            packageName = pkgName,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            category = notification.category ?: "SYSTEM",
            channelId = notification.channelId,
            channelName = channelName,
            key = sbn.key ?: "${pkgName}_${sbn.id}_${sbn.postTime}",
            extrasJson = extrasJson
        ).copy(
            notificationId = sbn.id,
            tag = sbn.tag,
            groupKey = sbn.groupKey,
            sortKey = notification.sortKey,
            summaryText = summaryText,
            infoText = infoText,
            appIconPath = appIconPath,
            largeIconPath = largeIconPath,
            picturePath = picturePath,
            postTime = sbn.postTime,
            importance = importance,
            priority = notification.priority,
            isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0,
            isClearable = sbn.isClearable,
            isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            isAutoCancel = (flags and Notification.FLAG_AUTO_CANCEL) != 0,
            isOnlyAlertOnce = (flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0,
            hasSound = hasSound,
            hasVibrate = hasVibrate,
            hasLights = hasLights,
            actionsJson = actionsArray.toString(),
            actionLabels = actionTitles.joinToString(", "),
            messagingMessagesJson = messagesArray.toString(),
            peopleListJson = peopleArray.toString(),
            isGroupConversation = isGroupConversation
        )

        val storedLink = if (extractedLink != null) {
            extractedLink
        } else {
            null
        }

        serviceScope.launch {
            try {
                val highConfidenceExisting = database.notificationDao()
                    .getHighConfidenceDeepLink(pkgName, sbn.groupKey)
                val shouldKeepExisting =
                    highConfidenceExisting?.deepLinkUri != null &&
                        highConfidenceExisting.deepLinkConfidence == "HIGH" &&
                        (extractedLink == null ||
                            extractedLink.confidence != com.example.data.LinkConfidence.HIGH)
                database.notificationDao().insert(
                    parsedEntity.copy(
                        deepLinkUri = if (shouldKeepExisting) highConfidenceExisting?.deepLinkUri else storedLink?.uri,
                        deepLinkSource = if (shouldKeepExisting) highConfidenceExisting?.deepLinkSource else storedLink?.source?.name,
                        deepLinkConfidence = if (shouldKeepExisting) highConfidenceExisting?.deepLinkConfidence else storedLink?.confidence?.name
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) liveRegistry.remove(sbn.key)
        // Never delete from vault archive.
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
