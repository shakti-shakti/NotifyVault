package com.example.service

import android.content.Context
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
            NotifyVaultNotificationListenerService.requestRebind(applicationContext)
        } else {
            android.util.Log.i("NotifyVault", "Listener heartbeat: connected")
        }
        return Result.success()
    }
}