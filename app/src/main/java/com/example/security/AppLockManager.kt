package com.example.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

enum class AutoLockTimeout(val label: String, val durationMs: Long) {
    IMMEDIATELY("Immediately on Minimize", 0L),
    SECONDS_30("30 Seconds", 30_000L),
    MINUTES_1("1 Minute (Recommended)", 60_000L),
    MINUTES_5("5 Minutes", 300_000L);

    companion object {
        fun fromName(name: String?): AutoLockTimeout {
            return entries.firstOrNull { it.name == name } ?: MINUTES_1
        }
    }
}

class AppLockManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var inactivityJob: Job? = null

    private val _isPinLockEnabled = MutableStateFlow(false)
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _timeoutOption = MutableStateFlow(AutoLockTimeout.MINUTES_1)
    val timeoutOption: StateFlow<AutoLockTimeout> = _timeoutOption.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(true)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    private var lastActiveTime: Long = System.currentTimeMillis()
    private var backgroundedTime: Long = 0L

    init {
        val enabled = prefs.getBoolean(KEY_PIN_ENABLED, false) && prefs.getString(KEY_PIN_HASH, null) != null
        _isPinLockEnabled.value = enabled
        _timeoutOption.value = AutoLockTimeout.fromName(prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.MINUTES_1.name))
        _isPrivacyModeEnabled.value = prefs.getBoolean(KEY_PRIVACY_MODE, true)

        if (enabled) {
            // Initially lock app when opened
            _isAppLocked.value = true
        }
        startInactivityChecker()
    }

    /**
     * Hashes the raw PIN with a cryptographically secure random salt using SHA-256.
     * The raw PIN is never stored in plaintext on disk.
     */
    private fun hashPin(pin: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$pin"
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Sets a new 4-digit PIN, hashes it with a fresh salt, and enables PIN lock.
     */
    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()

        _isPinLockEnabled.value = true
        _isAppLocked.value = false
        lastActiveTime = System.currentTimeMillis()
        return true
    }

    /**
     * Verifies the entered PIN against the stored cryptographic salt and hash.
     */
    fun verifyPin(inputPin: String): Boolean {
        if (!_isPinLockEnabled.value) return true
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val computedHash = hashPin(inputPin, storedSalt)
        val matches = MessageDigest.isEqual(computedHash.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))

        if (matches) {
            _isAppLocked.value = false
            lastActiveTime = System.currentTimeMillis()
        }
        return matches
    }

    /**
     * Disables PIN Lock and erases the stored credentials.
     */
    fun disablePin(currentPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()

        _isPinLockEnabled.value = false
        _isAppLocked.value = false
        return true
    }

    /**
     * Changes current PIN to a new 4-digit PIN.
     */
    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        return setPin(newPin)
    }

    /**
     * Updates the auto-lock inactivity timeout.
     */
    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        _timeoutOption.value = timeout
        prefs.edit().putString(KEY_AUTO_LOCK_TIMEOUT, timeout.name).apply()
    }

    /**
     * Toggles Privacy Screen (FLAG_SECURE to hide screen in recent apps switcher).
     */
    fun setPrivacyModeEnabled(enabled: Boolean, activity: Activity? = null) {
        _isPrivacyModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
        activity?.let { applyPrivacyFlag(it) }
    }

    fun applyPrivacyFlag(activity: Activity) {
        val shouldSecure = _isPinLockEnabled.value && _isPrivacyModeEnabled.value
        if (shouldSecure) {
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Immediately locks the app.
     */
    fun lockNow() {
        if (_isPinLockEnabled.value) {
            _isAppLocked.value = true
        }
    }

    /**
     * Called on any touch/interaction event in the activity to update the activity timestamp.
     */
    fun onUserInteraction() {
        lastActiveTime = System.currentTimeMillis()
    }

    /**
     * Called when Activity onPause / onStop is triggered (minimized or backgrounded).
     */
    fun onAppBackgrounded() {
        backgroundedTime = System.currentTimeMillis()
        if (_isPinLockEnabled.value && _timeoutOption.value == AutoLockTimeout.IMMEDIATELY) {
            _isAppLocked.value = true
        }
    }

    /**
     * Called when Activity onResume / onStart is triggered (restored from background).
     */
    fun onAppForegrounded() {
        if (_isPinLockEnabled.value && !_isAppLocked.value) {
            val elapsed = System.currentTimeMillis() - backgroundedTime
            if (_timeoutOption.value == AutoLockTimeout.IMMEDIATELY || elapsed >= _timeoutOption.value.durationMs) {
                _isAppLocked.value = true
            }
        }
        lastActiveTime = System.currentTimeMillis()
    }

    /**
     * Periodic background check for 1-minute inactivity while the app remains open.
     */
    private fun startInactivityChecker() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            while (isActive) {
                delay(5_000L) // Check every 5 seconds
                if (_isPinLockEnabled.value && !_isAppLocked.value) {
                    val timeoutMs = _timeoutOption.value.durationMs
                    if (timeoutMs > 0 && System.currentTimeMillis() - lastActiveTime >= timeoutMs) {
                        _isAppLocked.value = true
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "finance_app_lock_prefs"
        private const val KEY_PIN_ENABLED = "key_pin_enabled"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val KEY_PRIVACY_MODE = "key_privacy_mode"

        @Volatile
        private var instance: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return instance ?: synchronized(this) {
                instance ?: AppLockManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
