package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.security.LockManager

/**
 * Keeps screen-off locking reliable even when the activity is not in memory.
 * MainActivity mirrors this state into its ViewModel when it resumes.
 */
class LockBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == Intent.ACTION_SCREEN_OFF) {
            val lockManager = LockManager.getInstance(context)
            if (lockManager.isLockOnScreenOff()) {
                lockManager.lock()
            }
        }
    }
}