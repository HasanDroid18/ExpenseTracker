package com.example.expensetracker.auth.biometric

import android.content.Context
import android.content.SharedPreferences

/**
 * BiometricPreferenceManager manages user preferences for biometric authentication.
 *
 * This manager:
 * - Stores whether biometric lock is enabled/disabled
 * - Allows users to toggle biometric security on/off
 * - Uses SharedPreferences for persistent storage
 */
class BiometricPreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "biometric_security_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    /**
     * Check if biometric security is enabled by the user
     * Default is false (disabled) - users must explicitly enable security
     */
    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Enable or disable biometric security
     *
     * @param enabled true to enable, false to disable
     */
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    /**
     * Toggle biometric security state
     *
     * @return new state (true if now enabled, false if now disabled)
     */
    fun toggleBiometric(): Boolean {
        val newState = !isBiometricEnabled()
        setBiometricEnabled(newState)
        return newState
    }
}

