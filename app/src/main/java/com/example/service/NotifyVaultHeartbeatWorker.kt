package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotifyVaultHeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val connected = NotifyVaultNotificationListenerService.isConnected()
        if (!connected) {
            android.util.Log.i("NotifyVault", "Listener heartbeat: disconnected; requesting rebind")
            runCatching {
                val component = ComponentName(applicationContext, NotifyVaultNotificationListenerService::class.java)
                val pm = applicationContext.packageManager
                pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }
        } else {
            android.util.Log.i("NotifyVault", "Listener heartbeat: connected")
        }
        return Result.success()
    }
}