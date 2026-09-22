package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class LockMethod(val title: String) {
    NONE("No Lock"),
    PIN("4–6 Digit PIN"),
    PASSWORD("Alphanumeric Password"),
    PATTERN("3x3 Pattern Lock")
}

sealed class CooldownStatus {
    object None : CooldownStatus()
    data class Active(val remainingSeconds: Int) : CooldownStatus()
    object RequiresResetPhrase : CooldownStatus()
}

data class PasswordStrength(
    val score: Int, // 0 to 4
    val label: String,
    val feedback: String
)

class LockManager(private val context: Context) {

    private val prefs: SharedPreferences = createPreferences()

    private val _isLocked = MutableStateFlow(prefs.getBoolean(KEY_IS_LOCKED_INIT, true))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _lockMethodFlow = MutableStateFlow(getLockMethod())
    val lockMethodFlow: StateFlow<LockMethod> = _lockMethodFlow.asStateFlow()

    private val _biometricEnabledFlow = MutableStateFlow(isBiometricEnabled())
    val biometricEnabledFlow: StateFlow<Boolean> = _biometricEnabledFlow.asStateFlow()

    private val _hideRecentsFlow = MutableStateFlow(isHideContentInRecents())
    val hideRecentsFlow: StateFlow<Boolean> = _hideRecentsFlow.asStateFlow()

    private fun createPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "notify_vault_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("notify_vault_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun isLockConfigured(): Boolean {
        val method = getLockMethod()
        val hash = prefs.getString(KEY_HASH, null)
        return method != LockMethod.NONE && !hash.isNullOrBlank()
    }

    fun getLockMethod(): LockMethod {
        val name = prefs.getString(KEY_METHOD, LockMethod.NONE.name)
        return try {
            LockMethod.valueOf(name ?: LockMethod.NONE.name)
        } catch (e: Exception) {
            LockMethod.NONE
        }
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _biometricEnabledFlow.value = enabled
    }

    // Auto-lock options: 0 (Immediately), 15000, 30000, 60000, 300000, -1 (Never)
    fun getAutoLockTimeout(): Long = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, 0L)
    fun setAutoLockTimeout(timeoutMs: Long) {
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, timeoutMs).apply()
    }

    fun isLockOnScreenOff(): Boolean = prefs.getBoolean(KEY_LOCK_ON_SCREEN_OFF, true)
    fun setLockOnScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ON_SCREEN_OFF, enabled).apply()
    }

    fun isLockOnAppClose(): Boolean = prefs.getBoolean(KEY_LOCK_ON_APP_CLOSE, true)
    fun setLockOnAppClose(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ON_APP_CLOSE, enabled).apply()
    }

    fun isHideContentInRecents(): Boolean = prefs.getBoolean(KEY_HIDE_RECENTS, true)
    fun setHideContentInRecents(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_RECENTS, enabled).apply()
        _hideRecentsFlow.value = enabled
    }

    fun isBlurSensitiveContent(): Boolean = prefs.getBoolean(KEY_BLUR_SENSITIVE, true)
    fun setBlurSensitiveContent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLUR_SENSITIVE, enabled).apply()
    }

    private var lastPausedTime: Long = 0L

    fun recordAppPaused() {
        lastPausedTime = System.currentTimeMillis()
        if (isLockOnAppClose() && isLockConfigured()) {
            lock()
        }
    }

    fun shouldLockOnResume(): Boolean {
        if (!isLockConfigured()) return false
        if (_isLocked.value) return true
        val timeout = getAutoLockTimeout()
        if (timeout == 0L) return true
        if (timeout > 0 && lastPausedTime > 0) {
            val elapsed = System.currentTimeMillis() - lastPausedTime
            if (elapsed >= timeout) return true
        }
        return false
    }

    fun lock() {
        if (isLockConfigured()) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        resetFailedAttempts()
    }

    // --- PBKDF2 HASHING WITH RANDOM SALT ---
    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashWithSalt(input: String, salt: ByteArray): String {
        val spec = PBEKeySpec(input.toCharArray(), salt, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(hash)
        } else {
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        }
    }

    fun setCredential(method: LockMethod, input: String) {
        val salt = generateSalt()
        val saltStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(salt)
        } else {
            android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        }
        val hash = hashWithSalt(input, salt)

        prefs.edit()
            .putString(KEY_METHOD, method.name)
            .putString(KEY_SALT, saltStr)
            .putString(KEY_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()

        _lockMethodFlow.value = method
    }

    fun verifyCredential(input: String): Boolean {
        val saltStr = prefs.getString(KEY_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_HASH, null) ?: return false

        val salt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getDecoder().decode(saltStr)
        } else {
            android.util.Base64.decode(saltStr, android.util.Base64.NO_WRAP)
        }

        val actualHash = hashWithSalt(input, salt)
        val matches = actualHash == expectedHash

        if (matches) {
            resetFailedAttempts()
        } else {
            recordFailedAttempt()
        }
        return matches
    }

    fun changeCredential(oldInput: String, newMethod: LockMethod, newInput: String): Boolean {
        if (!verifyCredential(oldInput)) {
            return false
        }
        setCredential(newMethod, newInput)
        return true
    }

    fun disableLock(currentInput: String): Boolean {
        if (!verifyCredential(currentInput)) {
            return false
        }
        prefs.edit()
            .putString(KEY_METHOD, LockMethod.NONE.name)
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()

        _lockMethodFlow.value = LockMethod.NONE
        _biometricEnabledFlow.value = false
        _isLocked.value = false
        return true
    }

    // --- FAILED ATTEMPTS & COOLDOWN LOGIC ---
    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun getCooldownStatus(): CooldownStatus {
        val cooldownUntil = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        val now = System.currentTimeMillis()
        val attempts = getFailedAttempts()

        if (attempts >= 15) {
            return CooldownStatus.RequiresResetPhrase
        }

        if (cooldownUntil > now) {
            val remainingSec = ((cooldownUntil - now) / 1000).toInt() + 1
            return CooldownStatus.Active(remainingSec)
        }
        return CooldownStatus.None
    }

    private fun recordFailedAttempt() {
        val current = getFailedAttempts() + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current)
        val now = System.currentTimeMillis()

        when {
            current in 5..9 -> {
                editor.putLong(KEY_COOLDOWN_UNTIL, now + 30_000L) // 30 seconds
            }
            current in 10..14 -> {
                editor.putLong(KEY_COOLDOWN_UNTIL, now + 300_000L) // 5 minutes
            }
            current >= 15 -> {
                editor.putLong(KEY_COOLDOWN_UNTIL, now + 86400_000L) // Require "reset" phrase
            }
        }
        editor.apply()
    }

    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()
    }

    fun unlockViaResetPhrase(phrase: String): Boolean {
        if (phrase.trim().equals("reset", ignoreCase = true)) {
            resetFailedAttempts()
            return true
        }
        return false
    }

    // --- EMERGENCY WIPE ---
    fun resetAllLockData() = emergencyWipe()

    fun emergencyWipe() {
        try {
            // Clear preferences
            prefs.edit().clear().apply()
            // Reset lock state
            _lockMethodFlow.value = LockMethod.NONE
            _biometricEnabledFlow.value = false
            _isLocked.value = false

            // Clear internal app_icons directory
            val iconDir = File(context.filesDir, "app_icons")
            if (iconDir.exists()) {
                iconDir.deleteRecursively()
            }
            val picDir = File(context.filesDir, "notification_pictures")
            if (picDir.exists()) {
                picDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- PASSWORD STRENGTH EVALUATOR ---
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(0, "Empty", "Enter a password")
        }
        var score = 0
        if (password.length >= 6) score++
        if (password.length >= 10) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1 -> PasswordStrength(1, "Weak", "Add numbers and make it longer")
            2 -> PasswordStrength(2, "Fair", "Mix in symbols or uppercase letters")
            3 -> PasswordStrength(3, "Good", "Strong password with mixed characters")
            else -> PasswordStrength(4, "Excellent", "Vault-grade security!")
        }
    }

    companion object {
        private const val KEY_IS_LOCKED_INIT = "key_is_locked_init"
        private const val KEY_METHOD = "key_lock_method"
        private const val KEY_HASH = "key_credential_hash"
        private const val KEY_SALT = "key_credential_salt"
        private const val KEY_BIOMETRIC = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val KEY_LOCK_ON_SCREEN_OFF = "key_lock_on_screen_off"
        private const val KEY_LOCK_ON_APP_CLOSE = "key_lock_on_app_close"
        private const val KEY_HIDE_RECENTS = "key_hide_recents"
        private const val KEY_BLUR_SENSITIVE = "key_blur_sensitive"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "key_cooldown_until"

        @Volatile
        private var instance: LockManager? = null

        fun getInstance(context: Context): LockManager {
            return instance ?: synchronized(this) {
                instance ?: LockManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
