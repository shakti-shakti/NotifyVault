package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

enum class TextMatchType(val label: String) {
    CONTAINS("Contains"),
    STARTS_WITH("Starts with"),
    ENDS_WITH("Ends with"),
    REGEX("Regex")
}

data class ExcludeTextRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pattern: String,
    val matchType: TextMatchType = TextMatchType.CONTAINS,
    val isCaseSensitive: Boolean = false,
    val isEnabled: Boolean = true
)

data class ExcludedNotificationLogItem(
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val title: String,
    val reason: String
)

class ExcludeRulesRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notify_vault_exclude_rules", Context.MODE_PRIVATE)

    // Master switch
    private val _isMasterFilteringEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_FILTERING, true))
    val isMasterFilteringEnabled: StateFlow<Boolean> = _isMasterFilteringEnabled.asStateFlow()

    // Excluded Packages
    private val _excludedPackages = MutableStateFlow(loadExcludedPackages())
    val excludedPackages: StateFlow<Set<String>> = _excludedPackages.asStateFlow()

    // Text & Keyword & Regex Rules
    private val _textRules = MutableStateFlow(loadTextRules())
    val textRules: StateFlow<List<ExcludeTextRule>> = _textRules.asStateFlow()

    // Excluded categories
    private val _excludedCategories = MutableStateFlow(loadExcludedCategories())
    val excludedCategories: StateFlow<Set<String>> = _excludedCategories.asStateFlow()

    // Priority filter (0 to 5)
    private val _minPriority = MutableStateFlow(prefs.getInt(KEY_MIN_PRIORITY, -2))
    val minPriority: StateFlow<Int> = _minPriority.asStateFlow()

    // Quiet Hours
    private val _isQuietHoursEnabled = MutableStateFlow(prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false))
    val isQuietHoursEnabled: StateFlow<Boolean> = _isQuietHoursEnabled.asStateFlow()

    private val _quietHoursStartMinute = MutableStateFlow(prefs.getInt(KEY_QUIET_START_MIN, 22 * 60)) // 22:00
    val quietHoursStartMinute: StateFlow<Int> = _quietHoursStartMinute.asStateFlow()

    private val _quietHoursEndMinute = MutableStateFlow(prefs.getInt(KEY_QUIET_END_MIN, 7 * 60)) // 07:00
    val quietHoursEndMinute: StateFlow<Int> = _quietHoursEndMinute.asStateFlow()

    // Repeat duplicate window (in minutes, 0 = off)
    private val _repeatWindowMinutes = MutableStateFlow(prefs.getInt(KEY_REPEAT_WINDOW_MIN, 0))
    val repeatWindowMinutes: StateFlow<Int> = _repeatWindowMinutes.asStateFlow()

    // Toggles for Ongoing, Media, System
    private val _excludeOngoing = MutableStateFlow(prefs.getBoolean(KEY_EXCLUDE_ONGOING, true)) // Default true
    val excludeOngoing: StateFlow<Boolean> = _excludeOngoing.asStateFlow()

    private val _excludeMedia = MutableStateFlow(prefs.getBoolean(KEY_EXCLUDE_MEDIA, true))
    val excludeMedia: StateFlow<Boolean> = _excludeMedia.asStateFlow()

    private val _excludeSystem = MutableStateFlow(prefs.getBoolean(KEY_EXCLUDE_SYSTEM, false))
    val excludeSystem: StateFlow<Boolean> = _excludeSystem.asStateFlow()

    // Statistics: Excluded count today
    private val _excludedTodayCount = MutableStateFlow(getExcludedTodayCountInternal())
    val excludedTodayCount: StateFlow<Int> = _excludedTodayCount.asStateFlow()

    // Recent exclusion logs (in memory, up to 50 items)
    private val _recentExclusions = MutableStateFlow<List<ExcludedNotificationLogItem>>(emptyList())
    val recentExclusions: StateFlow<List<ExcludedNotificationLogItem>> = _recentExclusions.asStateFlow()

    // Recent notification cache for repeat duplicate detection (key = packageName:title:text -> timestamp)
    private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()

    fun setMasterFilteringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_FILTERING, enabled).apply()
        _isMasterFilteringEnabled.value = enabled
    }

    fun setExcludedPackages(packages: Set<String>) {
        val jsonArray = JSONArray(packages)
        prefs.edit().putString(KEY_EXCLUDED_PACKAGES, jsonArray.toString()).apply()
        _excludedPackages.value = packages
    }

    fun addExcludedPackage(pkg: String) {
        val updated = _excludedPackages.value.toMutableSet().apply { add(pkg) }
        setExcludedPackages(updated)
    }

    fun removeExcludedPackage(pkg: String) {
        val updated = _excludedPackages.value.toMutableSet().apply { remove(pkg) }
        setExcludedPackages(updated)
    }

    private fun loadExcludedPackages(): Set<String> {
        val json = prefs.getString(KEY_EXCLUDED_PACKAGES, null) ?: return emptySet()
        return try {
            val arr = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun addTextRule(rule: ExcludeTextRule) {
        val current = _textRules.value.toMutableList()
        current.add(0, rule)
        saveTextRules(current)
    }

    fun updateTextRule(updatedRule: ExcludeTextRule) {
        val current = _textRules.value.map { if (it.id == updatedRule.id) updatedRule else it }
        saveTextRules(current)
    }

    fun deleteTextRule(ruleId: String) {
        val current = _textRules.value.filter { it.id != ruleId }
        saveTextRules(current)
    }

    private fun saveTextRules(rules: List<ExcludeTextRule>) {
        val arr = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("pattern", rule.pattern)
                put("matchType", rule.matchType.name)
                put("isCaseSensitive", rule.isCaseSensitive)
                put("isEnabled", rule.isEnabled)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TEXT_RULES, arr.toString()).apply()
        _textRules.value = rules
    }

    private fun loadTextRules(): List<ExcludeTextRule> {
        val json = prefs.getString(KEY_TEXT_RULES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ExcludeTextRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExcludeTextRule(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        pattern = obj.getString("pattern"),
                        matchType = try {
                            TextMatchType.valueOf(obj.optString("matchType", TextMatchType.CONTAINS.name))
                        } catch (e: Exception) {
                            TextMatchType.CONTAINS
                        },
                        isCaseSensitive = obj.optBoolean("isCaseSensitive", false),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setExcludedCategories(categories: Set<String>) {
        val arr = JSONArray(categories)
        prefs.edit().putString(KEY_EXCLUDED_CATEGORIES, arr.toString()).apply()
        _excludedCategories.value = categories
    }

    private fun loadExcludedCategories(): Set<String> {
        val json = prefs.getString(KEY_EXCLUDED_CATEGORIES, null)
        if (json == null) {
            // Default ongoing excluded
            return setOf("ONGOING")
        }
        return try {
            val arr = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            set
        } catch (e: Exception) {
            setOf("ONGOING")
        }
    }

    fun setMinPriority(priority: Int) {
        prefs.edit().putInt(KEY_MIN_PRIORITY, priority).apply()
        _minPriority.value = priority
    }

    fun setQuietHours(enabled: Boolean, startMin: Int, endMin: Int) {
        prefs.edit()
            .putBoolean(KEY_QUIET_HOURS_ENABLED, enabled)
            .putInt(KEY_QUIET_START_MIN, startMin)
            .putInt(KEY_QUIET_END_MIN, endMin)
            .apply()
        _isQuietHoursEnabled.value = enabled
        _quietHoursStartMinute.value = startMin
        _quietHoursEndMinute.value = endMin
    }

    fun setRepeatWindowMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_REPEAT_WINDOW_MIN, minutes).apply()
        _repeatWindowMinutes.value = minutes
    }

    fun setExcludeOngoing(exclude: Boolean) {
        prefs.edit().putBoolean(KEY_EXCLUDE_ONGOING, exclude).apply()
        _excludeOngoing.value = exclude
    }

    fun setExcludeMedia(exclude: Boolean) {
        prefs.edit().putBoolean(KEY_EXCLUDE_MEDIA, exclude).apply()
        _excludeMedia.value = exclude
    }

    fun setExcludeSystem(exclude: Boolean) {
        prefs.edit().putBoolean(KEY_EXCLUDE_SYSTEM, exclude).apply()
        _excludeSystem.value = exclude
    }

    // --- EVALUATION ENGINE (CALLED AT NOTIFICATION CAPTURE) ---
    /**
     * Returns null if allowed, or reason string if excluded.
     */
    fun shouldExclude(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        bigText: String,
        category: String,
        isOngoing: Boolean,
        priority: Int
    ): String? {
        if (!_isMasterFilteringEnabled.value) return null

        // 1. Exclude Ongoing
        if (_excludeOngoing.value && isOngoing) {
            return recordExclusion(appName, packageName, title, "Excluded ongoing/persistent status")
        }

        // 2. Exclude Media
        if (_excludeMedia.value && (category.equals("transport", ignoreCase = true) || category.equals("media", ignoreCase = true))) {
            return recordExclusion(appName, packageName, title, "Excluded media playback control")
        }

        // 3. Exclude System
        if (_excludeSystem.value && (category.equals("system", ignoreCase = true) || packageName.startsWith("android") || packageName.startsWith("com.android"))) {
            return recordExclusion(appName, packageName, title, "Excluded system background alert")
        }

        // 4. Exclude by App package
        if (_excludedPackages.value.contains(packageName)) {
            return recordExclusion(appName, packageName, title, "App excluded in capture rules")
        }

        // 5. Exclude by Category
        if (_excludedCategories.value.contains(category.uppercase())) {
            return recordExclusion(appName, packageName, title, "Category '$category' excluded")
        }

        // 6. Exclude by Priority
        if (priority < _minPriority.value) {
            return recordExclusion(appName, packageName, title, "Priority below threshold (${priority} < ${_minPriority.value})")
        }

        // 7. Exclude by Time (Quiet Hours)
        if (_isQuietHoursEnabled.value) {
            val cal = Calendar.getInstance()
            val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startMin = _quietHoursStartMinute.value
            val endMin = _quietHoursEndMinute.value

            val inQuietHours = if (startMin <= endMin) {
                currentMinuteOfDay in startMin..endMin
            } else {
                currentMinuteOfDay >= startMin || currentMinuteOfDay <= endMin
            }

            if (inQuietHours) {
                return recordExclusion(appName, packageName, title, "Received during Quiet Hours")
            }
        }

        // 8. Exclude by Repeat / Identical Notification
        val repeatMin = _repeatWindowMinutes.value
        if (repeatMin > 0) {
            val key = "$packageName:$title:$text"
            val now = System.currentTimeMillis()
            val lastSeen = recentNotificationTimestamps[key]
            if (lastSeen != null && (now - lastSeen) < (repeatMin * 60_000L)) {
                recentNotificationTimestamps[key] = now
                return recordExclusion(appName, packageName, title, "Duplicate received within $repeatMin min")
            }
            recentNotificationTimestamps[key] = now
        }

        // 9. Exclude by Text / Keyword / Regex
        val combinedText = "$title $text $bigText"
        for (rule in _textRules.value) {
            if (!rule.isEnabled || rule.pattern.isBlank()) continue

            val matched = try {
                when (rule.matchType) {
                    TextMatchType.CONTAINS -> combinedText.contains(rule.pattern, ignoreCase = !rule.isCaseSensitive)
                    TextMatchType.STARTS_WITH -> {
                        title.startsWith(rule.pattern, ignoreCase = !rule.isCaseSensitive) ||
                                text.startsWith(rule.pattern, ignoreCase = !rule.isCaseSensitive)
                    }
                    TextMatchType.ENDS_WITH -> {
                        title.endsWith(rule.pattern, ignoreCase = !rule.isCaseSensitive) ||
                                text.endsWith(rule.pattern, ignoreCase = !rule.isCaseSensitive)
                    }
                    TextMatchType.REGEX -> {
                        val regexOptions = if (rule.isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                        rule.pattern.toRegex(regexOptions).containsMatchIn(combinedText)
                    }
                }
            } catch (e: Exception) {
                false
            }

            if (matched) {
                return recordExclusion(appName, packageName, title, "Matched text rule: '${rule.pattern}'")
            }
        }

        return null
    }

    private fun recordExclusion(appName: String, packageName: String, title: String, reason: String): String {
        incrementExcludedCountToday()
        val item = ExcludedNotificationLogItem(
            appName = appName,
            packageName = packageName,
            title = title.ifBlank { "Notification" },
            reason = reason
        )
        val currentLogs = _recentExclusions.value.toMutableList()
        currentLogs.add(0, item)
        if (currentLogs.size > 50) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _recentExclusions.value = currentLogs
        return reason
    }

    private fun getExcludedTodayCountInternal(): Int {
        val todayKey = getTodayDateKey()
        return prefs.getInt("key_excluded_cnt_$todayKey", 0)
    }

    private fun incrementExcludedCountToday() {
        val todayKey = getTodayDateKey()
        val key = "key_excluded_cnt_$todayKey"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()
        _excludedTodayCount.value = count
    }

    private fun getTodayDateKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    companion object {
        private const val KEY_MASTER_FILTERING = "key_master_filtering"
        private const val KEY_EXCLUDED_PACKAGES = "key_excluded_packages"
        private const val KEY_TEXT_RULES = "key_text_rules"
        private const val KEY_EXCLUDED_CATEGORIES = "key_excluded_categories"
        private const val KEY_MIN_PRIORITY = "key_min_priority"
        private const val KEY_QUIET_HOURS_ENABLED = "key_quiet_hours_enabled"
        private const val KEY_QUIET_START_MIN = "key_quiet_start_min"
        private const val KEY_QUIET_END_MIN = "key_quiet_end_min"
        private const val KEY_REPEAT_WINDOW_MIN = "key_repeat_window_min"
        private const val KEY_EXCLUDE_ONGOING = "key_exclude_ongoing"
        private const val KEY_EXCLUDE_MEDIA = "key_exclude_media"
        private const val KEY_EXCLUDE_SYSTEM = "key_exclude_system"

        @Volatile
        private var instance: ExcludeRulesRepository? = null

        fun getInstance(context: Context): ExcludeRulesRepository {
            return instance ?: synchronized(this) {
                instance ?: ExcludeRulesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
