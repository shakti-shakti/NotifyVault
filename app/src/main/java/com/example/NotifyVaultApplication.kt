package com.example

import android.app.Application
import com.example.data.VaultDatabase
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.NotifyVaultHeartbeatWorker
import java.util.concurrent.TimeUnit

class NotifyVaultApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize database instance safely
        try {
            VaultDatabase.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notifyvault_listener_heartbeat",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NotifyVaultHeartbeatWorker>(15, TimeUnit.MINUTES).build()
        )
    }
}
