package com.example.expensetracker.AppScreens.Goals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Monthly Expense Goal UI state and operations
 * Provides LiveData for UI observation and functions for goal operations
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val repository: GoalRepository,
    private val goalDataStore: ExpenseGoalDataStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    // LiveData for goal amount from DataStore
    val goalAmount: LiveData<Double?> = goalDataStore.goalAmountFlow.asLiveData()

    // LiveData for save/update operation result
    private val _saveGoalState = MutableLiveData<Result<String>?>()
    val saveGoalState: LiveData<Result<String>?> = _saveGoalState

    // LiveData for delete operation result
    private val _deleteGoalState = MutableLiveData<Result<String>?>()
    val deleteGoalState: LiveData<Result<String>?> = _deleteGoalState

    // LiveData for current month expenses
    private val _currentExpenses = MutableLiveData<Double>()
    val currentExpenses: LiveData<Double> = _currentExpenses

    // LiveData for progress percentage (0-100+)
    private val _progressPercent = MutableLiveData<Int>()
    val progressPercent: LiveData<Int> = _progressPercent

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Save or update monthly expense goal
     * @param goalAmount The goal amount to save (must be positive)
     */
    fun saveGoal(goalAmount: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Validate goal amount
                if (goalAmount <= 0) {
                    _saveGoalState.value = Result.failure(
                        Exception("Goal amount must be greater than zero")
                    )
                    return@launch
                }

                // Save goal via repository
                val result = repository.saveGoal(goalAmount)
                _saveGoalState.value = result

                // Refresh expenses and progress after saving goal
                if (result.isSuccess) {
                    refreshExpensesAndProgress()
                }
            } catch (e: Exception) {
                _saveGoalState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete the current goal
     */
    fun deleteGoal() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val result = repository.deleteGoal()
                _deleteGoalState.value = result

                // Clear UI state
                if (result.isSuccess) {
                    _currentExpenses.value = 0.0
                    _progressPercent.value = 0
                }
            } catch (e: Exception) {
                _deleteGoalState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear save goal state (call after consuming the result)
     * Prevents observer from re-triggering on fragment recreation
     */
    fun clearSaveGoalState() {
        _saveGoalState.value = null
    }

    /**
     * Clear delete goal state (call after consuming the result)
     * Prevents observer from re-triggering on fragment recreation
     */
    fun clearDeleteGoalState() {
        _deleteGoalState.value = null
    }

    /**
     * Refresh current month expenses and progress
     * Call this when screen is opened or after adding transactions
     */
    fun refreshExpensesAndProgress() {
        viewModelScope.launch {
            try {
                android.util.Log.d("GoalViewModel", "🔄 Refreshing expenses and progress...")
                _isLoading.value = true

                // Check if month changed and reset if needed
                if (repository.shouldResetForNewMonth()) {
                    android.util.Log.d("GoalViewModel", "📅 Month changed - resetting progress")
                    repository.resetForNewMonth()
                }

                // Get goal amount
                val goal = repository.getGoalAmount()
                android.util.Log.d("GoalViewModel", "🎯 Goal amount: $$goal")

                if (goal != null && goal > 0) {
                    // Get current expenses
                    val expenses = repository.getCurrentMonthExpenses()
                    android.util.Log.d("GoalViewModel", "💰 Current expenses: $$expenses")
                    _currentExpenses.value = expenses

                    // Calculate progress
                    val progress = repository.calculateProgress(expenses, goal)
                    android.util.Log.d("GoalViewModel", "📊 Progress: $progress% ($expenses / $goal)")
                    _progressPercent.value = progress

                    // Check for milestone notifications (in case WorkManager hasn't run yet)
                    checkMilestones(progress, goal, expenses)
                } else {
                    android.util.Log.d("GoalViewModel", "⚠️ No goal set")
                    // No goal set
                    _currentExpenses.value = 0.0
                    _progressPercent.value = 0
                }
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "❌ Error refreshing: ${e.message}", e)
                // Handle error silently or show error state
                _currentExpenses.value = 0.0
                _progressPercent.value = 0
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if milestones have been reached and send notifications immediately
     * Sends notifications while user is using the app
     */
    private suspend fun checkMilestones(progress: Int, goal: Double, expenses: Double) {
        // Check 20% milestone
        if (repository.shouldNotifyForMilestone(progress, 20)) {
            android.util.Log.d("GoalViewModel", "🔔 20% milestone reached - sending notification")
            GoalNotificationBuilder.sendNotification20Percent(context, goal, expenses)
            repository.markMilestoneNotified(20)
        }

        // Check 50% milestone
        if (repository.shouldNotifyForMilestone(progress, 50)) {
            android.util.Log.d("GoalViewModel", "🔔 50% milestone reached - sending notification")
            GoalNotificationBuilder.sendNotification50Percent(context, goal, expenses)
            repository.markMilestoneNotified(50)
        }

        // Check 100% milestone
        if (repository.shouldNotifyForMilestone(progress, 100)) {
            android.util.Log.d("GoalViewModel", "🔔 100% milestone reached - sending notification")
            GoalNotificationBuilder.sendNotification100Percent(context, goal, expenses)
            repository.markMilestoneNotified(100)
        }
    }

    /**
     * Calculate remaining budget
     * @return Remaining amount (can be negative if overspent)
     */
    fun getRemainingBudget(): LiveData<Double> {
        val result = MutableLiveData<Double>()
        viewModelScope.launch {
            val goal = repository.getGoalAmount() ?: 0.0
            val expenses = _currentExpenses.value ?: 0.0
            result.value = goal - expenses
        }
        return result
    }

    /**
     * Check if goal is exceeded
     * @return true if expenses exceed goal
     */
    fun isGoalExceeded(): Boolean {
        val goal = goalAmount.value ?: 0.0
        val expenses = _currentExpenses.value ?: 0.0
        return expenses > goal && goal > 0
    }

    /**
     * Get progress color based on percentage
     * @return Color resource ID for progress indicator
     */
    fun getProgressColor(): Int {
        return when (val progress = _progressPercent.value ?: 0) {
            in 0..49 -> android.R.color.holo_green_dark // Green: under 50%
            in 50..79 -> android.R.color.holo_orange_dark // Orange: 50-79%
            else -> android.R.color.holo_red_dark // Red: 80%+
        }
    }
}

