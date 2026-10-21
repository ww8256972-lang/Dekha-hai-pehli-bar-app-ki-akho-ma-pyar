package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SecurityPinManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aslam_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_IS_INITIAL_PIN = "is_initial_pin"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"

        const val DEFAULT_INITIAL_PIN = "8055"
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
    }

    init {
        // Initialize with default hashed PIN "8055" if not already set
        if (!prefs.contains(KEY_PIN_HASH)) {
            val salt = generateSalt()
            val hash = hashPin(DEFAULT_INITIAL_PIN, salt)
            prefs.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_IS_INITIAL_PIN, true)
                .putBoolean(KEY_APP_LOCK_ENABLED, true)
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .apply()
        }
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isInitialPin(): Boolean = prefs.getBoolean(KEY_IS_INITIAL_PIN, true)

    fun isLockedOut(): Pair<Boolean, Long> {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val currentTime = System.currentTimeMillis()
        return if (currentTime < lockoutUntil) {
            val remainingSec = (lockoutUntil - currentTime) / 1000
            Pair(true, remainingSec)
        } else {
            Pair(false, 0L)
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val (lockedOut, _) = isLockedOut()
        if (lockedOut) return false

        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val savedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val enteredHash = hashPin(enteredPin, salt)

        return if (enteredHash == savedHash) {
            // Reset failed attempts on success
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            true
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed)
            if (failed >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
            }
            editor.apply()
            false
        }
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        if (newPin.length < 4) return false

        val newSalt = generateSalt()
        val newHash = hashPin(newPin, newSalt)
        prefs.edit()
            .putString(KEY_PIN_SALT, newSalt)
            .putString(KEY_PIN_HASH, newHash)
            .putBoolean(KEY_IS_INITIAL_PIN, false)
            .apply()
        return true
    }

    private fun generateSalt(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin:aslam_record_salt"
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
