package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.data.AppInfoResolver
import com.example.data.DeepLinkExtractor
import com.example.data.NotificationEntity
import com.example.data.NotificationHistoryEntity
import com.example.data.NotificationRepository
import com.example.data.NotificationFeaturePreferences
import com.example.data.OtpCatcherPreferences
import com.example.data.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class NotifyVaultNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processingMutex = Mutex()
    private val lastOtpBySource = ConcurrentHashMap<String, Pair<String, Long>>()
    private lateinit var database: VaultDatabase
    private lateinit var deduper: NotificationDeduper
    private lateinit var otpNotifier: OtpNotifier
    private lateinit var otpPreferences: OtpCatcherPreferences
    private lateinit var featurePreferences: NotificationFeaturePreferences
    private val liveRegistry = LiveNotificationRegistry.getInstance()

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = VaultDatabase.getInstance(applicationContext)
        deduper = NotificationDeduper(database.notificationDao())
        otpNotifier = OtpNotifier(applicationContext)
        otpPreferences = OtpCatcherPreferences.getInstance(applicationContext)
        featurePreferences = NotificationFeaturePreferences.getInstance(applicationContext)
        createNotificationChannels()
        startCaptureForeground()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        startCaptureForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        liveRegistry.clear()
        serviceScope.cancel()
        if (instance == this) instance = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        startCaptureForeground()
        rescanActiveNotifications()
    }

    override fun onListenerDisconnected() {
        liveRegistry.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == applicationContext.packageName) return
        super.onNotificationPosted(sbn)
        processStatusBarNotification(sbn)
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification) {
        val pkgName = sbn.packageName
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: Bundle()
        val fingerprint = NotificationFingerprint.of(sbn)
        val liveItem = LiveNotificationRegistry.fromStatusBarNotification(sbn)
        liveRegistry.put(liveItem)

        val extractedLink = DeepLinkExtractor.extract(notification, pkgName, applicationContext)
        val resolvedApp = AppInfoResolver.resolveSync(applicationContext, pkgName, database)
        val appName = resolvedApp.appName
        val appIconPath = resolvedApp.iconPath

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

        var largeIconPath: String? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.getLargeIcon()?.loadDrawable(applicationContext)?.let { drawable ->
                    (drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                        largeIconPath = AppInfoResolver.saveBitmapToFile(
                            applicationContext, "notification_images",
                            "large_${pkgName}_${sbn.id}_${sbn.postTime}", bitmap
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)?.let { bitmap ->
                    largeIconPath = AppInfoResolver.saveBitmapToFile(
                        applicationContext, "notification_images",
                        "large_${pkgName}_${sbn.id}_${sbn.postTime}", bitmap
                    )
                }
            }
        } catch (_: Exception) {
        }

        var picturePath: String? = null
        try {
            @Suppress("DEPRECATION")
            extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)?.let { bitmap ->
                picturePath = AppInfoResolver.saveBitmapToFile(
                    applicationContext, "notification_images",
                    "pic_${pkgName}_${sbn.id}_${sbn.postTime}", bitmap
                )
            }
        } catch (_: Exception) {
        }

        var channelName: String? = null
        var importance = 3
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId?.let { channelId ->
                getSystemService(NotificationManager::class.java)?.getNotificationChannel(channelId)?.let { channel ->
                    channelName = channel.name?.toString()
                    importance = channel.importance
                }
            }
        }

        val flags = notification.flags
        val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val actionsArray = JSONArray()
        val actionTitles = mutableListOf<String>()
        notification.actions?.forEach { action ->
            val actionTitle = action.title?.toString().orEmpty()
            actionsArray.put(JSONObject().put("title", actionTitle))
            if (actionTitle.isNotBlank()) actionTitles += actionTitle
        }

        val messagesArray = JSONArray()
        try {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.forEach { value ->
                if (value is Bundle) {
                    messagesArray.put(JSONObject().apply {
                        put("text", value.getCharSequence("text")?.toString().orEmpty())
                        put("time", value.getLong("time"))
                        put(
                            "sender",
                            value.getCharSequence("sender")?.toString()
                                ?: value.getBundle("sender_person")?.getCharSequence("name")?.toString().orEmpty()
                        )
                    })
                }
            }
        } catch (_: Exception) {
        }

        val peopleArray = JSONArray()
        try {
            extras.getStringArrayList(Notification.EXTRA_PEOPLE)?.forEach { peopleArray.put(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                extras.getParcelableArrayList<android.app.Person>(Notification.EXTRA_PEOPLE_LIST)
                    ?.forEach { person -> peopleArray.put(person.name?.toString() ?: person.uri.orEmpty()) }
            }
        } catch (_: Exception) {
        }

        val extrasJson = JSONObject().apply {
            extras.keySet()?.forEach { key ->
                val value = extras.get(key)
                if (value != null && value !is ByteArray) put(key, value.toString().take(250))
            }
        }.toString()

        val baseEntity = NotificationRepository.parseNotification(
            packageName = pkgName,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            category = notification.category ?: "SYSTEM",
            channelId = notification.channelId,
            channelName = channelName,
            key = sbn.key ?: fingerprint.key,
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
            isOngoing = isOngoing,
            isClearable = sbn.isClearable,
            isGroupSummary = isGroupSummary,
            isAutoCancel = (flags and Notification.FLAG_AUTO_CANCEL) != 0,
            isOnlyAlertOnce = (flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0,
            hasSound = notification.sound != null || (notification.defaults and Notification.DEFAULT_SOUND) != 0,
            hasVibrate = notification.vibrate != null || (notification.defaults and Notification.DEFAULT_VIBRATE) != 0,
            hasLights = (flags and Notification.FLAG_SHOW_LIGHTS) != 0 || (notification.defaults and Notification.DEFAULT_LIGHTS) != 0,
            actionsJson = actionsArray.toString(),
            actionLabels = actionTitles.joinToString(", "),
            messagingMessagesJson = messagesArray.toString(),
            peopleListJson = peopleArray.toString(),
            isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false),
            fingerprintKey = fingerprint.key,
            contentHash = fingerprint.contentHash,
            lastSeenAt = fingerprint.updateTime
        )
        val otpMatch = OtpDetector.detect(baseEntity)
        val parsedEntity = baseEntity.copy(
            hasOtp = otpMatch != null,
            otpCode = otpMatch?.code,
            category = if (otpMatch != null) "OTP" else baseEntity.category
        )

        val excludeRules = com.example.data.ExcludeRulesRepository.getInstance(applicationContext)
        val excludeReason = excludeRules.shouldExclude(
            packageName = pkgName,
            appName = appName,
            title = title.orEmpty(),
            text = text.orEmpty(),
            bigText = bigText.orEmpty(),
            category = notification.category ?: "SYSTEM",
            isOngoing = isOngoing,
            priority = notification.priority
        )
        if (excludeReason != null) {
            if (otpMatch != null && otpPreferences.catchExcluded.value) maybeNotifyOtp(otpMatch)
            return
        }

        serviceScope.launch {
            processingMutex.withLock {
                try {
                    val start = System.nanoTime()
                    val decision = deduper.decide(fingerprint)
                    val effectiveDecision =
                        if (decision == DedupDecision.UpdateExisting && featurePreferences.showUpdatesSeparately.value) {
                            DedupDecision.InsertNew
                        } else decision
                    when (effectiveDecision) {
                        DedupDecision.InsertNew -> {
                            val storedLink = extractedLink
                            database.notificationDao().insert(
                                parsedEntity.copy(
                                    deepLinkUri = storedLink?.uri,
                                    deepLinkSource = storedLink?.source?.name,
                                    deepLinkConfidence = storedLink?.confidence?.name
                                )
                            )
                            otpMatch?.let { maybeNotifyOtp(it) }
                        }
                        DedupDecision.UpdateExisting -> {
                            val existing = deduper.existing(fingerprint)
                            if (existing == null) {
                                database.notificationDao().insert(parsedEntity)
                            } else {
                                database.notificationDao().insertHistory(
                                    NotificationHistoryEntity(
                                        parentId = existing.id,
                                        previousTitle = existing.title,
                                        previousText = existing.text,
                                        previousContentHash = existing.contentHash,
                                        replacedAt = fingerprint.updateTime
                                    )
                                )
                                database.notificationDao().update(
                                    parsedEntity.copy(
                                        id = existing.id,
                                        deepLinkUri = if (extractedLink?.confidence == com.example.data.LinkConfidence.HIGH) {
                                            extractedLink.uri
                                        } else existing.deepLinkUri,
                                        deepLinkSource = if (extractedLink?.confidence == com.example.data.LinkConfidence.HIGH) {
                                            extractedLink.source?.name
                                        } else existing.deepLinkSource,
                                        deepLinkConfidence = if (extractedLink?.confidence == com.example.data.LinkConfidence.HIGH) {
                                            extractedLink.confidence.name
                                        } else existing.deepLinkConfidence,
                                        isStarred = existing.isStarred,
                                        isArchived = existing.isArchived,
                                        isRead = existing.isRead,
                                        updateCount = existing.updateCount + 1,
                                        isUpdate = true,
                                        captureTime = fingerprint.updateTime,
                                        lastSeenAt = fingerprint.updateTime
                                    )
                                )
                                otpMatch?.let { maybeNotifyOtp(it) }
                            }
                        }
                        DedupDecision.SkipDuplicate -> Unit
                    }
                    val elapsedMs = (System.nanoTime() - start) / 1_000_000
                    if (elapsedMs > 50) android.util.Log.w(TAG, "Notification DB path took ${elapsedMs}ms")
                } catch (error: Exception) {
                    android.util.Log.e(TAG, "Unable to archive notification", error)
                }
            }
        }
    }

    private fun maybeNotifyOtp(match: OtpMatch) {
        if (!otpPreferences.otpEnabled.value) return
        if (match.sourcePackage in otpPreferences.excludedPackages.value && !otpPreferences.catchExcluded.value) return
        val selected = otpPreferences.selectedPackages.value
        if (selected.isNotEmpty() && match.sourcePackage !in selected) return
        val now = System.currentTimeMillis()
        val previous = lastOtpBySource[match.sourcePackage]
        if (previous != null && previous.first == match.code && now - previous.second < 60_000L) return
        lastOtpBySource[match.sourcePackage] = match.code to now

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotifyVault:OtpDelivery")
        try {
            wakeLock?.acquire(5_000L)
            otpNotifier.show(match)
            otpPreferences.addOtpLog(match.sourceApp, match.sourcePackage, match.code)
        } finally {
            if (wakeLock?.isHeld == true) wakeLock.release()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "NotifyVault Active", NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Quietly confirms that notification capture is active"
                    setShowBadge(false)
                }
            )
        }
        otpNotifier.ensureChannel()
    }

    private fun startCaptureForeground() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.example.R.drawable.ic_otp_lock)
                .setContentTitle("NotifyVault is active")
                .setContentText("Private notification capture is running")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Foreground capture notification unavailable", error)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) liveRegistry.remove(sbn.key)
    }

    companion object {
        const val CHANNEL_ID = "notifyvault_live_capture"
        const val NOTIFICATION_ID = 9010
        private const val TAG = "NotifyVault"
        @Volatile private var instance: NotifyVaultNotificationListenerService? = null

        fun rescanActiveNotifications() {
            runCatching {
                val service = instance ?: return
                service.activeNotifications?.forEach(service::processStatusBarNotification)
            }
        }

        fun isConnected(): Boolean = instance != null
    }
}