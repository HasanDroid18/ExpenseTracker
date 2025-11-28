package com.example.expensetracker.AppScreens.Goals

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker that runs periodically to:
 * 1. Check if month has changed and reset progress
 * 2. Check expense progress against goal
 * 3. Send notifications for milestones (50%, 80%, 100%)
 *
 * This worker runs in the background even when app is closed
 */
@HiltWorker
class GoalCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: GoalRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "GoalCheckWorker"
        const val WORK_NAME = "goal_check_work"
    }

    /**
     * Main work function that runs periodically
     * @return Result.success() if work completes successfully
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting goal check work...")

            // Step 1: Check if we need to reset for new month
            if (repository.shouldResetForNewMonth()) {
                Log.d(TAG, "New month detected - resetting progress")
                repository.resetForNewMonth()
            }

            // Step 2: Get goal amount
            val goalAmount = repository.getGoalAmount()

            // If no goal is set, skip the rest
            if (goalAmount == null || goalAmount <= 0) {
                Log.d(TAG, "No goal set - skipping milestone checks")
                return@withContext Result.success()
            }

            // Step 3: Get current month expenses
            val currentExpenses = repository.getCurrentMonthExpenses()

            // Step 4: Calculate progress percentage
            val progressPercent = repository.calculateProgress(currentExpenses, goalAmount)
            Log.d(TAG, "Progress: $progressPercent% ($currentExpenses / $goalAmount)")

            // Step 5: Check and send notifications for milestones
            checkAndNotifyMilestone(20, progressPercent, goalAmount, currentExpenses)
            checkAndNotifyMilestone(50, progressPercent, goalAmount, currentExpenses)
            checkAndNotifyMilestone(80, progressPercent, goalAmount, currentExpenses)
            checkAndNotifyMilestone(100, progressPercent, goalAmount, currentExpenses)

            Log.d(TAG, "Goal check work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in goal check work: ${e.message}", e)
            // Retry on failure
            Result.retry()
        }
    }

    /**
     * Check if milestone is reached and send notification if needed
     * @param milestone Milestone percentage (20, 50, 80, or 100)
     * @param currentProgress Current progress percentage
     * @param goalAmount Total goal amount
     * @param currentExpenses Current expense amount
     */
    private suspend fun checkAndNotifyMilestone(
        milestone: Int,
        currentProgress: Int,
        goalAmount: Double,
        currentExpenses: Double
    ) {
        // Check if we should notify for this milestone
        if (repository.shouldNotifyForMilestone(currentProgress, milestone)) {
            Log.d(TAG, "Sending $milestone% milestone notification")

            // Send appropriate notification
            when (milestone) {
                20 -> GoalNotificationBuilder.sendNotification20Percent(
                    context, goalAmount, currentExpenses
                )
                50 -> GoalNotificationBuilder.sendNotification50Percent(
                    context, goalAmount, currentExpenses
                )
                80 -> GoalNotificationBuilder.sendNotification80Percent(
                    context, goalAmount, currentExpenses
                )
                100 -> GoalNotificationBuilder.sendNotification100Percent(
                    context, goalAmount, currentExpenses
                )
            }

            // Mark milestone as notified
            repository.markMilestoneNotified(milestone)
        }
    }
}

