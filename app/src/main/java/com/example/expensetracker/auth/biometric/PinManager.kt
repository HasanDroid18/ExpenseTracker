package com.example.expensetracker.auth.biometric

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * PinManager handles in-app PIN authentication as a fallback
 * when biometric authentication is not available.
 *
 * This manager:
 * - Stores PIN securely using EncryptedSharedPreferences
 * - Verifies PIN attempts
 * - Checks if PIN is set
 * - Allows PIN updates
 */
class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_pin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN = "user_pin"
        private const val KEY_PIN_ENABLED = "pin_enabled"
    }

    /**
     * Check if a PIN has been set up
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.getBoolean(KEY_PIN_ENABLED, false) &&
                sharedPreferences.contains(KEY_PIN)
    }

    /**
     * Save a new PIN
     *
     * @param pin The PIN to save (should be validated before calling this)
     * @return true if saved successfully
     */
    fun savePin(pin: String): Boolean {
        return try {
            sharedPreferences.edit()
                .putString(KEY_PIN, pin)
                .putBoolean(KEY_PIN_ENABLED, true)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verify if the provided PIN matches the stored PIN
     *
     * @param pin The PIN to verify
     * @return true if PIN matches, false otherwise
     */
    fun verifyPin(pin: String): Boolean {
        if (!isPinSet()) return false

        val storedPin = sharedPreferences.getString(KEY_PIN, null)
        return storedPin == pin
    }

    /**
     * Clear the stored PIN (for PIN reset scenarios)
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    /**
     * Update existing PIN
     *
     * @param oldPin The current PIN for verification
     * @param newPin The new PIN to set
     * @return true if update successful, false if old PIN doesn't match
     */
    fun updatePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        return savePin(newPin)
    }
}

