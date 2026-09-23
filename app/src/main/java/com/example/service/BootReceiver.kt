package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.security.LockManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // A reboot is always a fresh lock boundary. This is intentionally
            // done before reconnecting the listener so no vault UI can reopen
            // unlocked after the device restarts.
            LockManager.getInstance(context).lock()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "notifyvault_listener_bootstrap",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NotifyVaultHeartbeatWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
            OtpNotifier(context).ensureChannel()
            // Ask Android to reconnect the already-authorized listener without
            // toggling its component state (which can revoke or delay access).
            NotifyVaultNotificationListenerService.requestRebind(context)
        }
    }
}
