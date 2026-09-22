package com.example.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class VaultLifecycleObserver(
    private val lockManager: LockManager,
    private val onLockRequired: () -> Unit
) : DefaultLifecycleObserver {

    private var lastBackgroundTimeMs: Long = 0L
    private var isColdStart: Boolean = true

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val now = System.currentTimeMillis()

        if (isColdStart) {
            isColdStart = false
            if (lockManager.isLockConfigured()) {
                onLockRequired()
            }
            return
        }

        if (lastBackgroundTimeMs > 0L) {
            val timeout = lockManager.getAutoLockTimeout()
            if (timeout != -1L) { // -1 means Never
                val elapsed = now - lastBackgroundTimeMs
                if (elapsed >= timeout && lockManager.isLockConfigured()) {
                    onLockRequired()
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        lastBackgroundTimeMs = System.currentTimeMillis()
        if (lockManager.isLockOnAppClose() && lockManager.isLockConfigured()) {
            onLockRequired()
        }
    }
}
