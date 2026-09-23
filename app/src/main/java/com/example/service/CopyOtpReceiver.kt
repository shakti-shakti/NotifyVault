package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.data.OtpCatcherPreferences

class CopyOtpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(OtpNotifier.EXTRA_NOTIFICATION_ID, 0)
        when (intent.action) {
            OtpNotifier.ACTION_COPY -> {
                val code = intent.getStringExtra(OtpNotifier.EXTRA_CODE).orEmpty()
                val copyValue = if (OtpCatcherPreferences.getInstance(context).copyWithAppName.value) {
                    val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
                    if (source.isBlank()) code else "$source: $code"
                } else code
                if (code.isNotBlank()) {
                    context.getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("OTP", copyValue))
                    Toast.makeText(context, "OTP copied", Toast.LENGTH_SHORT).show()
                    vibrate(context)
                }
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            OtpNotifier.ACTION_DISMISS -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120L)
        }
    }

    companion object {
        private const val EXTRA_SOURCE = "extra_source"
        fun copyIntent(context: Context, code: String, notificationId: Int, sourceApp: String): Intent =
            Intent(context, CopyOtpReceiver::class.java).apply {
                action = OtpNotifier.ACTION_COPY
                putExtra(OtpNotifier.EXTRA_CODE, code)
                putExtra(OtpNotifier.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_SOURCE, sourceApp)
            }
        fun dismissIntent(context: Context, notificationId: Int): Intent =
            Intent(context, CopyOtpReceiver::class.java).apply {
                action = OtpNotifier.ACTION_DISMISS
                putExtra(OtpNotifier.EXTRA_NOTIFICATION_ID, notificationId)
            }
    }
}