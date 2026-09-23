package com.example.service

import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object NotificationReliability {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            power.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    fun isBackgroundRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
    }

    fun isProtected(context: Context): Boolean =
        isIgnoringBatteryOptimizations(context) && !isBackgroundRestricted(context)

    fun openBatteryOptimizationRequest(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.getOrElse {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openAutoStartSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intents = when {
            manufacturer.contains("xiaomi") -> listOf(
                Intent("miui.intent.action.OP_AUTO_START").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("oppo") -> listOf(
                Intent("com.coloros.safecenter").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("vivo") -> listOf(
                Intent("com.vivo.permissionmanager").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("realme") -> listOf(
                Intent("com.oppo.safe").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("oneplus") -> listOf(
                Intent("com.oneplus.security.action.OP_AUTO_START").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("huawei") -> listOf(
                Intent("huawei.intent.action.HSM_BOOTAPP_MANAGER").setData(Uri.parse("package:${context.packageName}")),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            manufacturer.contains("samsung") -> listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
            else -> listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
            )
        }
        intents.firstOrNull { it.resolveActivity(context.packageManager) != null }?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openAppBatterySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}