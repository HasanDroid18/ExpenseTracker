package com.example.expensetracker.auth.biometric

import android.content.Context
import android.content.SharedPreferences

/**
 * PinAttemptManager handles PIN verification attempt tracking with throttling and lockout.
 *
 * Security Features:
 * - Tracks failed attempts
 * - Implements progressive delay after 3 failed attempts
 * - Temporary lockout after 5 failed attempts (30 seconds)
 * - Extended lockout after 10 failed attempts (5 minutes)
 * - Resets on successful authentication
 */
class PinAttemptManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "pin_attempt_prefs"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"

        // Throttling thresholds
        private const val DELAY_THRESHOLD = 3      // Start delay after 3 attempts
        private const val LOCKOUT_THRESHOLD = 5    // Lock for 30s after 5 attempts
        private const val EXTENDED_LOCKOUT_THRESHOLD = 10 // Lock for 5m after 10 attempts

        // Lockout durations (milliseconds)
        private const val SHORT_LOCKOUT_DURATION = 30_000L      // 30 seconds
        private const val EXTENDED_LOCKOUT_DURATION = 300_000L  // 5 minutes

        // Delay duration per attempt (after threshold)
        private const val DELAY_PER_ATTEMPT = 2_000L // 2 seconds
    }

    /**
     * Get current number of failed attempts
     */
    fun getFailedAttempts(): Int {
        return sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    /**
     * Record a failed PIN attempt
     */
    fun recordFailedAttempt() {
        val currentAttempts = getFailedAttempts()
        val newAttempts = currentAttempts + 1

        sharedPreferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, newAttempts)
            .apply()

        // Set lockout if threshold reached
        when {
            newAttempts >= EXTENDED_LOCKOUT_THRESHOLD -> {
                setLockout(EXTENDED_LOCKOUT_DURATION)
            }
            newAttempts >= LOCKOUT_THRESHOLD -> {
                setLockout(SHORT_LOCKOUT_DURATION)
            }
        }
    }

    /**
     * Reset failed attempts (call on successful authentication)
     */
    fun resetAttempts() {
        sharedPreferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    /**
     * Check if currently in lockout period
     *
     * @return true if locked out, false otherwise
     */
    fun isLockedOut(): Boolean {
        val lockoutUntil = sharedPreferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val currentTime = System.currentTimeMillis()

        return if (lockoutUntil > currentTime) {
            true
        } else {
            // Lockout expired, clear it
            if (lockoutUntil > 0L) {
                sharedPreferences.edit()
                    .putLong(KEY_LOCKOUT_UNTIL, 0L)
                    .apply()
            }
            false
        }
    }

    /**
     * Get remaining lockout time in milliseconds
     *
     * @return milliseconds remaining, or 0 if not locked out
     */
    fun getRemainingLockoutTime(): Long {
        val lockoutUntil = sharedPreferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val currentTime = System.currentTimeMillis()
        val remaining = lockoutUntil - currentTime
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Get delay before next attempt (progressive throttling)
     *
     * @return delay in milliseconds, or 0 if no delay
     */
    fun getAttemptDelay(): Long {
        val attempts = getFailedAttempts()
        return if (attempts >= DELAY_THRESHOLD) {
            // Progressive delay: 2s for 3rd attempt, 4s for 4th, etc.
            (attempts - DELAY_THRESHOLD + 1) * DELAY_PER_ATTEMPT
        } else {
            0L
        }
    }

    /**
     * Check if should show delay warning
     *
     * @return true if delay will be applied on next failure
     */
    fun shouldWarnAboutDelay(): Boolean {
        return getFailedAttempts() >= DELAY_THRESHOLD - 1
    }

    /**
     * Set lockout until specified duration from now
     */
    private fun setLockout(durationMillis: Long) {
        val lockoutUntil = System.currentTimeMillis() + durationMillis
        sharedPreferences.edit()
            .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            .apply()
    }

    /**
     * Get lockout message based on current state
     */
    fun getLockoutMessage(): String {
        val remainingMs = getRemainingLockoutTime()
        val remainingSeconds = (remainingMs / 1000).toInt()

        return when {
            remainingMs == 0L -> ""
            remainingSeconds < 60 -> "Too many attempts. Try again in $remainingSeconds seconds."
            else -> {
                val minutes = remainingSeconds / 60
                "Too many attempts. Try again in $minutes minute${if (minutes > 1) "s" else ""}."
            }
        }
    }

    /**
     * Get warning message for approaching lockout
     */
    fun getWarningMessage(): String {
        val attempts = getFailedAttempts()
        return when {
            attempts >= EXTENDED_LOCKOUT_THRESHOLD - 1 ->
                "Warning: 1 more wrong attempt will lock you out for 5 minutes!"
            attempts >= LOCKOUT_THRESHOLD - 1 ->
                "Warning: ${EXTENDED_LOCKOUT_THRESHOLD - attempts} more wrong attempts will lock you out!"
            attempts >= DELAY_THRESHOLD ->
                "Multiple failed attempts detected. Delays are now active."
            else -> ""
        }
    }
}

