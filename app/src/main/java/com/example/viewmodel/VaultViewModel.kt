package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.R
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import com.example.data.VaultDatabase
import com.example.service.NotifyVaultNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository
    private val database = VaultDatabase.getInstance(application)

    // Real Notification Listener Permission status
    private val _isListenerPermissionGranted = MutableStateFlow(false)
    val isListenerPermissionGranted: StateFlow<Boolean> = _isListenerPermissionGranted.asStateFlow()

    init {
        repository = NotificationRepository(database.notificationDao())
        checkNotificationPermission()
        // Synchronize any currently active notifications from the Android status bar
        syncActiveNotifications()
    }

    fun checkNotificationPermission() {
        try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(getApplication())
            val granted = enabledPackages.contains(getApplication<Application>().packageName)
            _isListenerPermissionGranted.value = granted
            if (granted) {
                syncActiveNotifications()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncActiveNotifications() {
        NotifyVaultNotificationListenerService.rescanActiveNotifications()
    }

    // Active Category Filter: "All", "OTPs", "Payments", "Deliveries", "Messages", "Starred"
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // App Lock State
    private val _isVaultLocked = MutableStateFlow(false)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

    // Live Capture Service Active
    private val _isCaptureActive = MutableStateFlow(true)
    val isCaptureActive: StateFlow<Boolean> = _isCaptureActive.asStateFlow()

    // All Real Active Notifications Flow
    val allNotifications: StateFlow<List<NotificationEntity>> = repository.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real Counts from Database
    val totalCount = repository.totalCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val otpCount = repository.otpCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val paymentCount = repository.paymentCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val starredCount = repository.starredCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val todayCount = repository.getTodayCount(getTodayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Real Notification List according to active filter and search query
    val notifications: StateFlow<List<NotificationEntity>> = combine(
        repository.activeNotifications,
        _selectedFilter,
        _searchQuery
    ) { all, filter, query ->
        var filtered = when (filter) {
            "OTPs" -> all.filter { it.hasOtp }
            "Payments" -> all.filter { it.hasAmount || it.category.equals("PAYMENT", ignoreCase = true) }
            "Deliveries" -> all.filter { it.category.equals("DELIVERY", ignoreCase = true) || it.text?.contains("deliver", ignoreCase = true) == true }
            "Messages" -> all.filter { it.category.equals("SOCIAL", ignoreCase = true) || it.category.equals("MESSAGES", ignoreCase = true) }
            "Starred" -> all.filter { it.isStarred }
            else -> all
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.title?.lowercase()?.contains(q) == true ||
                it.text?.lowercase()?.contains(q) == true ||
                it.appName.lowercase().contains(q) ||
                it.otpCode?.contains(q) == true ||
                it.senderName?.lowercase()?.contains(q) == true
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 100% REAL ANALYTICS CALCULATED FROM REAL NOTIFICATIONS:

    // 1. Real 14-day daily activity trajectory
    val realDailyActivity: StateFlow<List<Float>> = repository.activeNotifications.map { list ->
        val counts = FloatArray(14)
        val dayMillis = 24 * 60 * 60 * 1000L
        val todayEnd = getTodayStartMillis() + dayMillis
        for (notif in list) {
            val diffDays = ((todayEnd - notif.captureTime) / dayMillis).toInt()
            if (diffDays in 0 until 14) {
                val index = 13 - diffDays
                counts[index] += 1f
            }
        }
        counts.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(14) { 0f })

    // 2. Real 24-hour distribution heatmap
    val realHourlyCounts: StateFlow<List<Int>> = repository.activeNotifications.map { list ->
        val counts = IntArray(24)
        val cal = Calendar.getInstance()
        for (notif in list) {
            cal.timeInMillis = notif.captureTime
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                counts[hour] += 1
            }
        }
        counts.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(24) { 0 })

    // 3. Real Category Breakdown
    val realCategorySlices: StateFlow<List<Pair<String, Float>>> = repository.activeNotifications.map { list ->
        if (list.isEmpty()) {
            emptyList()
        } else {
            val grouped = list.groupBy { it.category.ifBlank { "General" } }
            grouped.map { (cat, items) ->
                cat to items.size.toFloat()
            }.sortedByDescending { it.second }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Real Top Apps by volume
    val realTopApps: StateFlow<List<Pair<String, Int>>> = repository.activeNotifications.map { list ->
        if (list.isEmpty()) {
            emptyList()
        } else {
            list.groupBy { it.appName.ifBlank { "Unknown" } }
                .map { (name, items) -> name to items.size }
                .sortedByDescending { it.second }
                .take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCaptureActive() {
        _isCaptureActive.value = !_isCaptureActive.value
    }

    fun toggleStar(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.setStarred(notification.id, !notification.isStarred)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun archiveNotification(id: Long) {
        viewModelScope.launch {
            repository.setArchived(id, true)
        }
    }

    fun toggleSelectionMode() {
        val newMode = !_isSelectionMode.value
        _isSelectionMode.value = newMode
        if (!newMode) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelectId(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun selectAll(allIds: List<Long>) {
        _selectedIds.value = allIds.toSet()
        _isSelectionMode.value = true
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.deleteByIds(ids)
            _selectedIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }

    fun starSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.starByIds(ids)
            _selectedIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }

    fun archiveSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.archiveByIds(ids)
            _selectedIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun lockVault() {
        _isVaultLocked.value = true
    }

    fun unlockVault() {
        _isVaultLocked.value = false
    }

    fun getNotificationById(id: Long) = repository.getById(id)

    // Generates a real Android notification to test live capture pipeline
    fun sendVerificationNotification() {
        val context = getApplication<Application>()
        val channelId = "notifyvault_real_test"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NotifyVault Live Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time verification alerts"
            }
            nm?.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val randomOtp = (100000..999999).random()
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Real-Time Verification: OTP $randomOtp")
            .setContentText("Your OTP code is $randomOtp. Captured live from Android status bar.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Your one-time passcode is $randomOtp. This real alert was issued by Android system and archived in real time by NotifyVault."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addExtras(android.os.Bundle().apply {
                putBoolean("IS_NOTIFY_VAULT_TEST", true)
            })
            .build()

        try {
            NotificationManagerCompat.from(context).notify(randomOtp, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VaultViewModel(application) as T
                }
            }
    }
}
