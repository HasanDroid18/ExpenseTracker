package com.example.expensetracker.AppScreens.Goals

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for managing Monthly Expense Goal data locally
 * Stores: goal amount, current month, last reset date, notification flags
 */
class ExpenseGoalDataStore(private val context: Context) {

    companion object {
        // Create DataStore instance
        private val Context.goalDataStore by preferencesDataStore("expense_goal_prefs")

        // Keys for storing goal data
        private val GOAL_AMOUNT_KEY = doublePreferencesKey("goal_amount")
        private val CURRENT_MONTH_KEY = intPreferencesKey("current_month")
        private val CURRENT_YEAR_KEY = intPreferencesKey("current_year")
        private val LAST_RESET_TIME_KEY = longPreferencesKey("last_reset_time")

        // Notification flags - track which notifications have been sent
        private val NOTIFIED_20_PERCENT_KEY = intPreferencesKey("notified_20_percent")
        private val NOTIFIED_50_PERCENT_KEY = intPreferencesKey("notified_50_percent")
        private val NOTIFIED_80_PERCENT_KEY = intPreferencesKey("notified_80_percent")
        private val NOTIFIED_100_PERCENT_KEY = intPreferencesKey("notified_100_percent")
    }

    /**
     * Save monthly expense goal amount
     * @param amount The goal amount to save
     */
    suspend fun saveGoalAmount(amount: Double) {
        context.goalDataStore.edit { prefs ->
            prefs[GOAL_AMOUNT_KEY] = amount
        }
    }

    /**
     * Get monthly expense goal amount as Flow
     * @return Flow of goal amount (null if not set)
     */
    val goalAmountFlow: Flow<Double?> = context.goalDataStore.data.map { prefs ->
        prefs[GOAL_AMOUNT_KEY]
    }

    /**
     * Save current month and year to track when to reset
     * @param month Current month (1-12)
     * @param year Current year
     */
    suspend fun saveCurrentMonthYear(month: Int, year: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[CURRENT_MONTH_KEY] = month
            prefs[CURRENT_YEAR_KEY] = year
        }
    }

    /**
     * Get saved month
     * @return Flow of month (null if not set)
     */
    val savedMonthFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[CURRENT_MONTH_KEY]
    }

    /**
     * Get saved year
     * @return Flow of year (null if not set)
     */
    val savedYearFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[CURRENT_YEAR_KEY]
    }

    /**
     * Save timestamp of last reset
     * @param timestamp Time in milliseconds
     */
    suspend fun saveLastResetTime(timestamp: Long) {
        context.goalDataStore.edit { prefs ->
            prefs[LAST_RESET_TIME_KEY] = timestamp
        }
    }

    /**
     * Get last reset time
     * @return Flow of timestamp
     */
    val lastResetTimeFlow: Flow<Long?> = context.goalDataStore.data.map { prefs ->
        prefs[LAST_RESET_TIME_KEY]
    }

    /**
     * Save notification flag for 20% milestone
     * Stores month+year as single int (e.g., 202411 for Nov 2024)
     */
    suspend fun saveNotified20Percent(monthYear: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[NOTIFIED_20_PERCENT_KEY] = monthYear
        }
    }

    /**
     * Get 20% notification flag
     */
    val notified20PercentFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[NOTIFIED_20_PERCENT_KEY]
    }

    /**
     * Save notification flag for 50% milestone
     * Stores month+year as single int (e.g., 202411 for Nov 2024)
     */
    suspend fun saveNotified50Percent(monthYear: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[NOTIFIED_50_PERCENT_KEY] = monthYear
        }
    }

    /**
     * Get 50% notification flag
     */
    val notified50PercentFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[NOTIFIED_50_PERCENT_KEY]
    }

    /**
     * Save notification flag for 80% milestone
     */
    suspend fun saveNotified80Percent(monthYear: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[NOTIFIED_80_PERCENT_KEY] = monthYear
        }
    }

    /**
     * Get 80% notification flag
     */
    val notified80PercentFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[NOTIFIED_80_PERCENT_KEY]
    }

    /**
     * Save notification flag for 100% milestone
     */
    suspend fun saveNotified100Percent(monthYear: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[NOTIFIED_100_PERCENT_KEY] = monthYear
        }
    }

    /**
     * Get 100% notification flag
     */
    val notified100PercentFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[NOTIFIED_100_PERCENT_KEY]
    }

    /**
     * Reset notification flags for new month
     * Called when month changes
     */
    suspend fun resetNotificationFlags() {
        context.goalDataStore.edit { prefs ->
            prefs.remove(NOTIFIED_20_PERCENT_KEY)
            prefs.remove(NOTIFIED_50_PERCENT_KEY)
            prefs.remove(NOTIFIED_80_PERCENT_KEY)
            prefs.remove(NOTIFIED_100_PERCENT_KEY)
        }
    }

    /**
     * Clear all goal data (for testing or reset)
     */
    suspend fun clearAllGoalData() {
        context.goalDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

