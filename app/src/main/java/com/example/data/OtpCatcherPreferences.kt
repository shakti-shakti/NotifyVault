package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class OtpLogEntry(
    val sourceApp: String,
    val sourcePackage: String,
    val maskedCode: String,
    val timestamp: Long
)

class OtpCatcherPreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notifyvault_otp", Context.MODE_PRIVATE)
    private val _otpEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val otpEnabled: StateFlow<Boolean> = _otpEnabled.asStateFlow()
    private val _useEmojiDigits = MutableStateFlow(prefs.getBoolean(KEY_EMOJI, true))
    val useEmojiDigits: StateFlow<Boolean> = _useEmojiDigits.asStateFlow()
    private val _copyWithAppName = MutableStateFlow(prefs.getBoolean(KEY_COPY_APP, false))
    val copyWithAppName: StateFlow<Boolean> = _copyWithAppName.asStateFlow()
    private val _timeoutMs = MutableStateFlow(prefs.getLong(KEY_TIMEOUT, 60_000L))
    val timeoutMs: StateFlow<Long> = _timeoutMs.asStateFlow()
    private val _soundMode = MutableStateFlow(prefs.getString(KEY_SOUND, "default") ?: "default")
    val soundMode: StateFlow<String> = _soundMode.asStateFlow()
    private val _soundUri = MutableStateFlow(prefs.getString(KEY_SOUND_URI, "") ?: "")
    val soundUri: StateFlow<String> = _soundUri.asStateFlow()
    private val _vibrationMode = MutableStateFlow(prefs.getString(KEY_VIBRATION, "short") ?: "short")
    val vibrationMode: StateFlow<String> = _vibrationMode.asStateFlow()
    private val _lockscreenMode = MutableStateFlow(prefs.getString(KEY_LOCKSCREEN, "hide") ?: "hide")
    val lockscreenMode: StateFlow<String> = _lockscreenMode.asStateFlow()
    private val _catchExcluded = MutableStateFlow(prefs.getBoolean(KEY_CATCH_EXCLUDED, true))
    val catchExcluded: StateFlow<Boolean> = _catchExcluded.asStateFlow()
    private val _selectedPackages = MutableStateFlow(loadSet(KEY_SELECTED))
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()
    private val _excludedPackages = MutableStateFlow(loadSet(KEY_EXCLUDED))
    val excludedPackages: StateFlow<Set<String>> = _excludedPackages.asStateFlow()
    private val _otpLog = MutableStateFlow(loadLog())
    val otpLog: StateFlow<List<OtpLogEntry>> = _otpLog.asStateFlow()

    fun setOtpEnabled(value: Boolean) = updateBoolean(KEY_ENABLED, value) { _otpEnabled.value = value }
    fun setUseEmojiDigits(value: Boolean) = updateBoolean(KEY_EMOJI, value) { _useEmojiDigits.value = value }
    fun setCopyWithAppName(value: Boolean) = updateBoolean(KEY_COPY_APP, value) { _copyWithAppName.value = value }
    fun setTimeout(value: Long) { prefs.edit().putLong(KEY_TIMEOUT, value).apply(); _timeoutMs.value = value }
    fun setSoundMode(value: String) { prefs.edit().putString(KEY_SOUND, value).apply(); _soundMode.value = value }
    fun setSoundUri(value: String) { prefs.edit().putString(KEY_SOUND_URI, value).apply(); _soundUri.value = value }
    fun setVibrationMode(value: String) { prefs.edit().putString(KEY_VIBRATION, value).apply(); _vibrationMode.value = value }
    fun setLockscreenMode(value: String) { prefs.edit().putString(KEY_LOCKSCREEN, value).apply(); _lockscreenMode.value = value }
    fun setCatchExcluded(value: Boolean) = updateBoolean(KEY_CATCH_EXCLUDED, value) { _catchExcluded.value = value }
    fun setSelectedPackages(value: Set<String>) { saveSet(KEY_SELECTED, value); _selectedPackages.value = value }
    fun setExcludedPackages(value: Set<String>) { saveSet(KEY_EXCLUDED, value); _excludedPackages.value = value }

    fun addOtpLog(sourceApp: String, sourcePackage: String, code: String) {
        val next = (listOf(OtpLogEntry(sourceApp, sourcePackage, mask(code), System.currentTimeMillis())) + _otpLog.value).take(20)
        val json = JSONArray().apply {
            next.forEach { item -> put(JSONObject().apply {
                put("app", item.sourceApp); put("package", item.sourcePackage)
                put("code", item.maskedCode); put("time", item.timestamp)
            }) }
        }
        prefs.edit().putString(KEY_LOG, json.toString()).apply()
        _otpLog.value = next
    }

    fun clearLog() { prefs.edit().remove(KEY_LOG).apply(); _otpLog.value = emptyList() }

    private fun mask(code: String): String =
        if (code.length <= 2) "•".repeat(code.length) else "•".repeat(code.length - 2) + code.takeLast(2)
    private fun updateBoolean(key: String, value: Boolean, update: () -> Unit) {
        prefs.edit().putBoolean(key, value).apply(); update()
    }
    private fun saveSet(key: String, values: Set<String>) = prefs.edit().putString(key, JSONArray(values.toList()).toString()).apply()
    private fun loadSet(key: String): Set<String> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { array.getString(it) }.toSet()
    }.getOrDefault(emptySet())
    private fun loadLog(): List<OtpLogEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY_LOG, "[]"))
        (0 until array.length()).map {
            val item = array.getJSONObject(it)
            OtpLogEntry(item.optString("app"), item.optString("package"), item.optString("code"), item.optLong("time"))
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_ENABLED = "enabled"; private const val KEY_EMOJI = "emoji"
        private const val KEY_COPY_APP = "copy_app"; private const val KEY_TIMEOUT = "timeout"
        private const val KEY_SOUND = "sound"; private const val KEY_SOUND_URI = "sound_uri"; private const val KEY_VIBRATION = "vibration"
        private const val KEY_LOCKSCREEN = "lockscreen"; private const val KEY_CATCH_EXCLUDED = "catch_excluded"
        private const val KEY_SELECTED = "selected"; private const val KEY_EXCLUDED = "excluded"; private const val KEY_LOG = "log"
        @Volatile private var instance: OtpCatcherPreferences? = null
        fun getInstance(context: Context): OtpCatcherPreferences =
            instance ?: synchronized(this) {
                instance ?: OtpCatcherPreferences(context).also { instance = it }
            }
    }
}