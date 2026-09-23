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
import com.example.data.CustomFilterChip
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import com.example.data.NotificationFeaturePreferences
import com.example.data.VaultDatabase
import com.example.service.NotifyVaultNotificationListenerService
import com.example.service.LiveNotificationRegistry
import com.example.data.ReplayEngine
import com.example.data.ReplayResult
import com.example.security.LockManager
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

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days")
}

data class TopAppStat(

    val appName: String,
    val packageName: String,
    val iconPath: String?,
    val count: Int
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository
    private val database = VaultDatabase.getInstance(application)
    val featurePreferences = NotificationFeaturePreferences.getInstance(application)
    private val liveRegistry = LiveNotificationRegistry.getInstance()
    private val replayEngine = ReplayEngine.getInstance(application)
    val liveNotificationKeys: StateFlow<Set<String>> = liveRegistry.liveKeys

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

    // Active Category Filter: "All", "OTPs", "Payments", "Deliveries", "Banking", "Social", "System", "Starred"
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Dashboard App Quick-Chip active package (or null for all apps)
    private val _selectedAppPackage = MutableStateFlow<String?>(null)
    val selectedAppPackage: StateFlow<String?> = _selectedAppPackage.asStateFlow()

    // Active Custom Filter Chip (or null)
    private val _selectedCustomChip = MutableStateFlow<CustomFilterChip?>(null)
    val selectedCustomChip: StateFlow<CustomFilterChip?> = _selectedCustomChip.asStateFlow()

    // Contextual Search Filters
    private val _searchDateRange = MutableStateFlow(DateRangeFilter.ALL)
    val searchDateRange: StateFlow<DateRangeFilter> = _searchDateRange.asStateFlow()

    private val _searchTypeFilters = MutableStateFlow<Set<String>>(emptySet())
    val searchTypeFilters: StateFlow<Set<String>> = _searchTypeFilters.asStateFlow()

    private val _searchSelectedApp = MutableStateFlow<String?>(null)
    val searchSelectedApp: StateFlow<String?> = _searchSelectedApp.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // App Lock State
    private val _isVaultLocked = MutableStateFlow(
        LockManager.getInstance(application).isLockConfigured()
    )
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

    // Real Notification List according to active filter, quick-chip, contextual
    // search scope, date range, type chips, and power syntax.
    private val baseNotifications = combine(
        repository.activeNotifications,
        _selectedFilter,
        _selectedAppPackage,
        _selectedCustomChip
    ) { all, filter, appPkg, customChip ->
        var list = all

        // 1. Dashboard App Quick-Chip filtering
        if (appPkg != null) {
            list = list.filter { it.packageName == appPkg }
        }

        // 2. Custom Filter Chip rule
        if (customChip != null) {
            list = list.filter { item ->
                val pkgMatch = customChip.packages.isEmpty() || customChip.packages.contains(item.packageName)
                val kwMatch = customChip.keywords.isEmpty() || customChip.keywords.any { kw ->
                    item.title?.contains(kw, ignoreCase = true) == true ||
                    item.text?.contains(kw, ignoreCase = true) == true
                }
                val regexMatch = customChip.regex.isNullOrBlank() || try {
                    val r = Regex(customChip.regex, RegexOption.IGNORE_CASE)
                    r.containsMatchIn(item.title ?: "") || r.containsMatchIn(item.text ?: "")
                } catch (e: Exception) { true }

                pkgMatch && kwMatch && regexMatch
            }
        } else {
            // Built-in Category Filter
            list = when (filter) {
                "OTPs", "OTP" -> list.filter { it.hasOtp || it.category.equals("OTP", ignoreCase = true) }
                "Payments", "Payment" -> list.filter { it.hasAmount || it.category.equals("PAYMENT", ignoreCase = true) }
                "Deliveries", "Delivery" -> list.filter { it.category.equals("DELIVERY", ignoreCase = true) || it.text?.contains("deliver", ignoreCase = true) == true }
                "Banking" -> list.filter { it.category.equals("BANKING", ignoreCase = true) || it.title?.contains("bank", ignoreCase = true) == true }
                "Messages", "Social" -> list.filter { it.category.equals("SOCIAL", ignoreCase = true) || it.category.equals("MESSAGES", ignoreCase = true) }
                "System" -> list.filter { it.category.equals("SYSTEM", ignoreCase = true) || it.packageName.startsWith("android") }
                "Starred" -> list.filter { it.isStarred }
                else -> list
            }
        }

        list
    }

    val notifications: StateFlow<List<NotificationEntity>> = combine(
        baseNotifications,
        _searchQuery,
        _searchSelectedApp,
        _searchDateRange,
        _searchTypeFilters
    ) { source, query, searchApp, dateRange, typeFilters ->
        var list = source

        if (searchApp != null) {
            list = list.filter { it.packageName == searchApp }
        }

        val dayMillis = 24 * 60 * 60 * 1000L
        val todayStart = getTodayStartMillis()
        list = when (dateRange) {
            DateRangeFilter.TODAY -> list.filter { it.captureTime >= todayStart }
            DateRangeFilter.YESTERDAY -> list.filter {
                it.captureTime in (todayStart - dayMillis) until todayStart
            }
            DateRangeFilter.LAST_7_DAYS -> list.filter {
                it.captureTime >= todayStart - (6 * dayMillis)
            }
            DateRangeFilter.ALL -> list
        }

        if (typeFilters.isNotEmpty()) {
            list = list.filter { item ->
                typeFilters.all { type ->
                    when (type) {
                        "OTP" -> item.hasOtp || item.category.equals("OTP", ignoreCase = true)
                        "Amounts" -> item.hasAmount
                        "Starred" -> item.isStarred
                        else -> true
                    }
                }
            }
        }

        if (query.isNotBlank()) {
            val tokens = query.trim().lowercase().split(Regex("\\s+"))
            val appSyntax = tokens.firstOrNull { it.startsWith("app:") }?.removePrefix("app:")
            if (!appSyntax.isNullOrBlank()) {
                list = list.filter {
                    it.appName.contains(appSyntax, ignoreCase = true) ||
                        it.packageName.contains(appSyntax, ignoreCase = true)
                }
            }
            if (tokens.any { it == "has:otp" }) {
                list = list.filter { it.hasOtp || it.category.equals("OTP", ignoreCase = true) }
            }
            if (tokens.any { it == "has:amount" }) {
                list = list.filter { it.hasAmount }
            }
            if (tokens.any { it == "is:starred" }) {
                list = list.filter { it.isStarred }
            }

            val freeText = tokens
                .filterNot { it == "has:otp" || it == "has:amount" || it == "is:starred" || it.startsWith("app:") || it.startsWith("amount:") }
                .joinToString(" ")
            if (freeText.isNotBlank()) {
                list = list.filter {
                    it.title?.contains(freeText, ignoreCase = true) == true ||
                        it.text?.contains(freeText, ignoreCase = true) == true ||
                        it.appName.contains(freeText, ignoreCase = true) ||
                        it.otpCode?.contains(freeText) == true ||
                        it.senderName?.contains(freeText, ignoreCase = true) == true
                }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAppPackage(pkg: String?) {
        _selectedAppPackage.value = if (_selectedAppPackage.value == pkg) null else pkg
    }

    fun selectCustomChip(chip: CustomFilterChip?) {
        _selectedCustomChip.value = chip
        if (chip != null) {
            _selectedFilter.value = chip.name
        }
    }

    fun setSearchDateRange(range: DateRangeFilter) {
        _searchDateRange.value = range
    }

    fun toggleSearchTypeFilter(type: String) {
        val current = _searchTypeFilters.value.toMutableSet()
        if (current.contains(type)) current.remove(type) else current.add(type)
        _searchTypeFilters.value = current
    }

    fun setSearchSelectedApp(pkg: String?) {
        _searchSelectedApp.value = if (_searchSelectedApp.value == pkg) null else pkg
    }

    fun clearSearchFilters() {
        _searchQuery.value = ""
        _searchSelectedApp.value = null
        _searchDateRange.value = DateRangeFilter.ALL
        _searchTypeFilters.value = emptySet()
    }

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
    val realTopApps: StateFlow<List<TopAppStat>> = repository.activeNotifications.map { list ->
        if (list.isEmpty()) {
            emptyList()
        } else {
            list.groupBy { it.packageName }
                .map { (pkg, items) ->
                    val first = items.first()
                    TopAppStat(
                        appName = first.appName.ifBlank { "Unknown" },
                        packageName = pkg,
                        iconPath = first.appIconPath,
                        count = items.size
                    )
                }
                .sortedByDescending { it.count }
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
        com.example.data.OtpCatcherPreferences.getInstance(getApplication()).clearLog()
    }

    fun unlockVault() {
        _isVaultLocked.value = false
    }

    fun getNotificationById(id: Long) = repository.getById(id)

    fun getNotificationHistory(id: Long) = repository.getHistory(id)

    fun replay(notification: NotificationEntity): ReplayResult =
        replayEngine.replay(notification)

    fun replayAction(notification: NotificationEntity, actionTitle: String): ReplayResult =
        replayEngine.replayAction(notification, actionTitle)

    fun isExactReplayReady(notification: NotificationEntity): Boolean =
        replayEngine.isExactReplayReady(notification)

    fun isActionLive(notification: NotificationEntity, actionTitle: String): Boolean =
        replayEngine.isActionLive(notification, actionTitle)

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
