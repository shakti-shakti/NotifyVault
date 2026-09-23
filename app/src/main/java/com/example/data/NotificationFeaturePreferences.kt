package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationFeaturePreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notifyvault_features", Context.MODE_PRIVATE)
    private val _showUpdatesSeparately = MutableStateFlow(prefs.getBoolean(KEY_SHOW_UPDATES, false))
    val showUpdatesSeparately: StateFlow<Boolean> = _showUpdatesSeparately.asStateFlow()

    fun setShowUpdatesSeparately(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_UPDATES, value).apply()
        _showUpdatesSeparately.value = value
    }

    companion object {
        private const val KEY_SHOW_UPDATES = "show_updates_separately"
        @Volatile private var instance: NotificationFeaturePreferences? = null
        fun getInstance(context: Context): NotificationFeaturePreferences =
            instance ?: synchronized(this) {
                instance ?: NotificationFeaturePreferences(context).also { instance = it }
            }
    }
}