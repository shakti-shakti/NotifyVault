package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.OtpCatcherPreferences

class OtpNotifier(private val context: Context) {
    private val prefs = OtpCatcherPreferences.getInstance(context)

    fun show(match: OtpMatch) {
        ensureChannel()
        val notificationId = notificationId(match)
        val displayCode = if (prefs.useEmojiDigits.value) EmojiDigits.convert(match.code) else match.code
        val copyIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            CopyOtpReceiver.copyIntent(context, match.code, notificationId, match.sourceApp),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            CopyOtpReceiver.dismissIntent(context, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_otp_lock)
            .setContentTitle("🔐  OTP from ${match.sourceApp}")
            .setContentText(displayCode)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$displayCode · Tap Copy to use."))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(0xFFE2A84B.toInt())
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setGroup(GROUP_KEY)
            .addAction(R.drawable.ic_otp_lock, "Copy", copyIntent)
            .addAction(R.drawable.ic_otp_lock, "Dismiss", dismissIntent)
        if (prefs.timeoutMs.value >= 0) builder.setTimeoutAfter(prefs.timeoutMs.value)
        if (prefs.lockscreenMode.value == "hide") builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
        else builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        loadSourceIcon(match.sourcePackage, match.sourceApp)?.let { builder.setLargeIcon(it) }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS is optional on Android 13; the listener itself remains usable.
        }
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "OTP Catcher", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Private one-time password alerts from NotifyVault"
                enableVibration(prefs.vibrationMode.value != "off")
                vibrationPattern = if (prefs.vibrationMode.value == "long") {
                    longArrayOf(0, 300, 120, 300)
                } else {
                    longArrayOf(0, 120, 80, 120)
                }
                setSound(
                    when (prefs.soundMode.value) {
                        "off" -> null
                        "custom" -> prefs.soundUri.value.takeIf { it.isNotBlank() }?.let(Uri::parse)
                        else -> android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                    },
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                lockscreenVisibility = if (prefs.lockscreenMode.value == "hide") {
                    Notification.VISIBILITY_SECRET
                } else Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun recreateChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.deleteNotificationChannel(CHANNEL_ID)
        }
        ensureChannel()
    }

    private fun loadSourceIcon(packageName: String, appName: String): Bitmap? {
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            val drawable = icon as? BitmapDrawable
            drawable?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun notificationId(match: OtpMatch): Int =
        (match.sourcePackage + ":" + match.code).hashCode() and 0x7fffffff

    companion object {
        const val CHANNEL_ID = "otp_catcher"
        const val GROUP_KEY = "notifyvault.otp"
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_COPY = "com.example.action.COPY_OTP"
        const val ACTION_DISMISS = "com.example.action.DISMISS_OTP"
    }
}