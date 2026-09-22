package com.example.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast

object OpenAppUseCase {

    fun openApp(
        context: Context,
        packageName: String,
        appName: String,
        onToastMessage: ((String) -> Unit)? = null
    ) {
        // Haptic feedback tick
        triggerHapticTick(context)

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            } catch (e: Exception) {
                // Fall back to settings
            }
        }

        // Fallback: Open system App Info / Settings screen
        try {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        } catch (e: ActivityNotFoundException) {
            val msg = "This app can't be opened directly."
            if (onToastMessage != null) {
                onToastMessage(msg)
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            val msg = "Unable to open $appName"
            if (onToastMessage != null) {
                onToastMessage(msg)
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerHapticTick(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20)
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }
}
