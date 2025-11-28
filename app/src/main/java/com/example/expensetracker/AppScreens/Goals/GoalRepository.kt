package com.example.expensetracker.AppScreens.Goals

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.AppScreens.History.TransactionResponse
import com.example.expensetracker.auth.UserDataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing expense goal operations
 * Handles goal CRUD, month checking, expense tracking, and progress calculation
 */
@Singleton
class GoalRepository @Inject constructor(
    private val api: ApiService,
    private val userDataStore: UserDataStore,
    private val goalDataStore: ExpenseGoalDataStore
) {

    companion object {
        private const val TAG = "GoalRepository"
    }

    /**
     * Save or update the monthly expense goal
     * @param goalAmount The goal amount to save
     * @return Result with success/failure
     */
    suspend fun saveGoal(goalAmount: Double): Result<String> {
        return try {
            // Save goal amount
            goalDataStore.saveGoalAmount(goalAmount)

            // Save current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // 0-based to 1-based
            val currentYear = calendar.get(Calendar.YEAR)
            goalDataStore.saveCurrentMonthYear(currentMonth, currentYear)

            // Save timestamp
            goalDataStore.saveLastResetTime(System.currentTimeMillis())

            // Reset notification flags for new goal
            goalDataStore.resetNotificationFlags()

            Log.d(TAG, "Goal saved: $$goalAmount for $currentMonth/$currentYear")
            Result.success("Goal saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save goal: ${e.message}", e)
            Result.failure(Exception("Failed to save goal: ${e.message}"))
        }
    }

    /**
     * Get the current goal amount
     * @return Goal amount or null if not set
     */
    suspend fun getGoalAmount(): Double? {
        return try {
            goalDataStore.goalAmountFlow.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get goal amount: ${e.message}", e)
            null
        }
    }

    /**
     * Check if we need to reset for a new month
     * Compares saved month/year with current month/year
     * @return true if reset is needed
     */
    suspend fun shouldResetForNewMonth(): Boolean {
        return try {
            val savedMonth = goalDataStore.savedMonthFlow.first()
            val savedYear = goalDataStore.savedYearFlow.first()

            // If no saved data, no reset needed
            if (savedMonth == null || savedYear == null) {
                return false
            }

            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            // Check if month or year has changed
            val needsReset = currentMonth != savedMonth || currentYear != savedYear

            if (needsReset) {
                Log.d(TAG, "Month changed: $savedMonth/$savedYear -> $currentMonth/$currentYear")
            }

            needsReset
        } catch (e: Exception) {
            Log.e(TAG, "Error checking month reset: ${e.message}", e)
            false
        }
    }

    /**
     * Reset progress for new month
     * Updates month/year and clears notification flags
     */
    suspend fun resetForNewMonth() {
        try {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            // Update saved month and year
            goalDataStore.saveCurrentMonthYear(currentMonth, currentYear)

            // Reset notification flags
            goalDataStore.resetNotificationFlags()

            // Update reset time
            goalDataStore.saveLastResetTime(System.currentTimeMillis())

            Log.d(TAG, "Reset for new month: $currentMonth/$currentYear")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset for new month: ${e.message}", e)
        }
    }

    /**
     * Get total expenses for current month from API
     * Only counts expenses AFTER the goal was created/reset
     * @return Total expense amount for current month (after goal creation)
     */
    suspend fun getCurrentMonthExpenses(): Double {
        return try {
            // Get auth token
            val token = userDataStore.tokenFlow.first()
            if (token == null) {
                Log.w(TAG, "No auth token found")
                return 0.0
            }

            // Get goal creation/reset timestamp
            val goalTimestamp = goalDataStore.lastResetTimeFlow.first() ?: 0L

            // If timestamp is 0, use start of current month (count all this month's expenses)
            val goalStartOfDay = if (goalTimestamp == 0L) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                Log.d(TAG, "⏰ No goal timestamp found, using start of month: ${java.util.Date(monthStart)}")
                monthStart
            } else {
                // Convert goal timestamp to start of day for fair comparison
                val goalCalendar = Calendar.getInstance().apply {
                    timeInMillis = goalTimestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayStart = goalCalendar.timeInMillis
                Log.d(TAG, "⏰ Goal created at: ${java.util.Date(goalTimestamp)}")
                Log.d(TAG, "⏰ Counting expenses from: ${java.util.Date(dayStart)} onwards")
                dayStart
            }

            // Fetch transactions from API
            val response = api.getTransactions("Bearer $token")

            if (response.isSuccessful) {
                val transactions = response.body() ?: emptyList()
                Log.d(TAG, "📦 Total transactions fetched: ${transactions.size}")

                // Get current month and year
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)
                Log.d(TAG, "📅 Current month/year: $currentMonth/$currentYear")

                // Debug: Log ALL transactions with full details
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "DEBUGGING ALL TRANSACTIONS:")
                transactions.forEachIndexed { index, t ->
                    Log.d(TAG, "[$index] Transaction: type='${t.type}', category='${t.category}', created_at='${t.created_at}', amount=${t.amount}, title='${t.title}'")
                }
                Log.d(TAG, "═══════════════════════════════════════")

                // Filter transactions: current month + expense type + AFTER goal creation
                Log.d(TAG, "🔍 Starting filter process...")
                val monthlyExpenses = transactions.filter { transaction ->
                    try {
                        Log.d(TAG, "→ Processing: ${transaction.title} (${transaction.created_at})")

                        // Parse transaction created_at date and timestamp
                        val dateStr = transaction.created_at.split("T")[0] // Get date part only
                        val dateParts = dateStr.split("-")

                        Log.d(TAG, "  Date string: $dateStr, parts: ${dateParts.joinToString()}")

                        if (dateParts.size >= 3) {
                            val transYear = dateParts[0].toIntOrNull() ?: 0
                            val transMonth = dateParts[1].toIntOrNull() ?: 0
                            val transDay = dateParts[2].toIntOrNull() ?: 0

                            Log.d(TAG, "  Parsed date: Year=$transYear, Month=$transMonth, Day=$transDay")

                            // Convert transaction date to timestamp for comparison
                            val transCalendar = Calendar.getInstance().apply {
                                set(transYear, transMonth - 1, transDay, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val transactionTimestamp = transCalendar.timeInMillis

                            // Check BOTH type and category to find expense indicator
                            val isExpense = transaction.type.equals("expense", ignoreCase = true) ||
                                          transaction.category.equals("expense", ignoreCase = true)
                            val isCurrentMonth = transYear == currentYear && transMonth == currentMonth
                            // Use start of day for goal to include all expenses from that day onwards
                            val isAfterGoalCreation = transactionTimestamp >= goalStartOfDay

                            val goalDate = java.util.Date(goalTimestamp)
                            val transDate = java.util.Date(transactionTimestamp)

                            Log.d(TAG, "  Type check: type='${transaction.type}', category='${transaction.category}', isExpense=$isExpense")
                            Log.d(TAG, "  Month check: trans=$transMonth, current=$currentMonth, isCurrentMonth=$isCurrentMonth")
                            Log.d(TAG, "  Time check: transDate=$transDate, goalStartDate=${java.util.Date(goalStartOfDay)}")
                            Log.d(TAG, "  Time check: transTimestamp=$transactionTimestamp, goalStartOfDay=$goalStartOfDay, isAfter=$isAfterGoalCreation")

                            val result = isExpense && isCurrentMonth && isAfterGoalCreation
                            Log.d(TAG, "  RESULT: ${if (result) "✅ INCLUDED" else "❌ EXCLUDED"} (expense=$isExpense, currentMonth=$isCurrentMonth, afterGoal=$isAfterGoalCreation)")

                            // Must be: expense + current month + created AFTER goal
                            result
                        } else {
                            Log.w(TAG, "⚠️ Invalid date format: ${transaction.created_at}")
                            false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing date: ${transaction.created_at}", e)
                        e.printStackTrace()
                        false
                    }
                }

                // Sum up expense amounts
                val totalExpenses = monthlyExpenses.sumOf { it.amount }

                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "FINAL RESULTS:")
                Log.d(TAG, "Total transactions fetched: ${transactions.size}")
                Log.d(TAG, "Transactions that passed filter: ${monthlyExpenses.size}")
                monthlyExpenses.forEach { t ->
                    Log.d(TAG, "  ✅ ${t.title}: $${t.amount} (${t.created_at})")
                }
                Log.d(TAG, "Total expenses: $$totalExpenses")
                Log.d(TAG, "═══════════════════════════════════════")

                totalExpenses
            } else {
                Log.e(TAG, "Failed to fetch transactions: ${response.code()}")
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current month expenses: ${e.message}", e)
            0.0
        }
    }

    /**
     * Calculate progress percentage based on expenses and goal
     * @param expenses Current month expenses
     * @param goal Goal amount
     * @return Progress percentage (0-100+)
     */
    fun calculateProgress(expenses: Double, goal: Double): Int {
        return if (goal > 0) {
            ((expenses / goal) * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Check if notification should be sent for a milestone
     * @param progressPercent Current progress percentage
     * @param milestone Milestone percentage (50, 80, or 100)
     * @return true if notification should be sent
     */
    suspend fun shouldNotifyForMilestone(progressPercent: Int, milestone: Int): Boolean {
        return try {
            // Get current month-year identifier (e.g., 202411)
            val calendar = Calendar.getInstance()
            val currentMonthYear = (calendar.get(Calendar.YEAR) * 100) +
                                   (calendar.get(Calendar.MONTH) + 1)

            // Check if we've reached the milestone
            if (progressPercent < milestone) {
                return false
            }

            // Check if we've already notified for this milestone this month
            val lastNotified = when (milestone) {
                20 -> goalDataStore.notified20PercentFlow.first()
                50 -> goalDataStore.notified50PercentFlow.first()
                80 -> goalDataStore.notified80PercentFlow.first()
                100 -> goalDataStore.notified100PercentFlow.first()
                else -> null
            }

            // Notify if we haven't notified this month for this milestone
            lastNotified != currentMonthYear
        } catch (e: Exception) {
            Log.e(TAG, "Error checking milestone notification: ${e.message}", e)
            false
        }
    }

    /**
     * Mark milestone as notified
     * @param milestone Milestone percentage (50, 80, or 100)
     */
    suspend fun markMilestoneNotified(milestone: Int) {
        try {
            val calendar = Calendar.getInstance()
            val currentMonthYear = (calendar.get(Calendar.YEAR) * 100) +
                                   (calendar.get(Calendar.MONTH) + 1)

            when (milestone) {
                20 -> goalDataStore.saveNotified20Percent(currentMonthYear)
                50 -> goalDataStore.saveNotified50Percent(currentMonthYear)
                80 -> goalDataStore.saveNotified80Percent(currentMonthYear)
                100 -> goalDataStore.saveNotified100Percent(currentMonthYear)
            }

            Log.d(TAG, "Marked $milestone% milestone as notified for month $currentMonthYear")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark milestone: ${e.message}", e)
        }
    }

    /**
     * Delete the goal
     */
    suspend fun deleteGoal(): Result<String> {
        return try {
            goalDataStore.clearAllGoalData()
            Log.d(TAG, "Goal deleted")
            Result.success("Goal deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete goal: ${e.message}", e)
            Result.failure(Exception("Failed to delete goal"))
        }
    }
}

