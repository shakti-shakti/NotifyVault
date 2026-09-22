package com.example

import android.app.Application
import com.example.data.VaultDatabase

class NotifyVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-initialize database instance safely
        try {
            VaultDatabase.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
