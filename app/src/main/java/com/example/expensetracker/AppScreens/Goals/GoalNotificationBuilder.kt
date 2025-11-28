package com.example.expensetracker.AppScreens.Goals

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R

/**
 * Helper class for building and sending expense goal notifications
 * Handles notification channels and milestone notifications
 */
object GoalNotificationBuilder {

    // Notification channel constants
    private const val CHANNEL_ID = "expense_goal_channel"
    private const val CHANNEL_NAME = "Expense Goal Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for expense goal milestones"

    // Notification IDs for different milestones
    private const val NOTIFICATION_ID_20 = 1001
    private const val NOTIFICATION_ID_50 = 1002
    private const val NOTIFICATION_ID_80 = 1003
    private const val NOTIFICATION_ID_100 = 1004

    /**
     * Create notification channel (required for Android 8.0+)
     * Should be called once when app starts
     * Properly configured for Android 12+ compatibility
     */
    fun createNotificationChannel(context: Context) {
        // Only create channel on Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                // Enable sound for notifications
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // Set LED color
                lightColor = android.graphics.Color.BLUE
                // Enable badge
                setShowBadge(true)
            }

            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            android.util.Log.d("GoalNotification", "Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Send notification for reaching 20% of goal
     * @param context Application context
     * @param goalAmount Total goal amount
     * @param currentExpenses Current expense amount
     */
    fun sendNotification20Percent(context: Context, goalAmount: Double, currentExpenses: Double) {
        val title = "20% of Monthly Goal Reached! 📊"
        val message = "You've spent $${String.format("%.2f", currentExpenses)} of your $${String.format("%.2f", goalAmount)} goal. Keep tracking!"

        sendNotification(context, NOTIFICATION_ID_20, title, message)
    }

    /**
     * Send notification for reaching 50% of goal
     * @param context Application context
     * @param goalAmount Total goal amount
     * @param currentExpenses Current expense amount
     */
    fun sendNotification50Percent(context: Context, goalAmount: Double, currentExpenses: Double) {
        val title = "50% of Monthly Goal Reached! ⚠️"
        val remaining = goalAmount - currentExpenses
        val message = "You're halfway there! $${String.format("%.2f", remaining)} remaining in your budget."

        sendNotification(context, NOTIFICATION_ID_50, title, message)
    }

    /**
     * Send notification for reaching 80% of goal
     * @param context Application context
     * @param goalAmount Total goal amount
     * @param currentExpenses Current expense amount
     */
    fun sendNotification80Percent(context: Context, goalAmount: Double, currentExpenses: Double) {
        val title = "80% of Monthly Goal Reached! ⚠️"
        val remaining = goalAmount - currentExpenses
        val message = "Watch out! Only $${String.format("%.2f", remaining)} left in your budget for this month."

        sendNotification(context, NOTIFICATION_ID_80, title, message)
    }

    /**
     * Send notification for reaching 100% of goal
     * @param context Application context
     * @param goalAmount Total goal amount
     * @param currentExpenses Current expense amount
     */
    fun sendNotification100Percent(context: Context, goalAmount: Double, currentExpenses: Double) {
        val title = "Monthly Goal Reached! 🚨"
        val overspent = currentExpenses - goalAmount
        val message = if (overspent > 0) {
            "You've exceeded your budget by $${String.format("%.2f", overspent)}. Review your expenses!"
        } else {
            "You've reached your monthly expense goal of $${String.format("%.2f", goalAmount)}."
        }

        sendNotification(context, NOTIFICATION_ID_100, title, message)
    }

    /**
     * Internal method to build and send notification
     * @param context Application context
     * @param notificationId Unique notification ID
     * @param title Notification title
     * @param message Notification message
     */
    private fun sendNotification(context: Context, notificationId: Int, title: String, message: String) {
        android.util.Log.d("GoalNotification", "📢 Sending notification: $title (Android API ${Build.VERSION.SDK_INT})")

        // Check if notifications are enabled (works on all Android versions)
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val areEnabled = notificationManager.areNotificationsEnabled()
            android.util.Log.d("GoalNotification", "Notifications enabled: $areEnabled")

            if (!areEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Only block on Android 13+ where permission is strictly enforced
                android.util.Log.w("GoalNotification", "⚠️ Notifications disabled - Android 13+ requires permission")
                return
            }
        } catch (e: Exception) {
            android.util.Log.w("GoalNotification", "Could not check notification status: ${e.message}")
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optional: Add extra to navigate to Goals screen
            putExtra("navigate_to", "goals")
        }

        // Create pending intent with proper flags for Android 12+
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires explicit mutability flag
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        // Build notification with proper configuration for Android 12+
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_add_24) // Use your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss when tapped
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibrate, lights
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Add vibration pattern (optional - setDefaults already enables vibration)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // For pre-Oreo, set vibration pattern directly
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        val notification = notificationBuilder.build()

        // Send notification
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("GoalNotification", "Notification sent: $title")
        } catch (e: SecurityException) {
            // Handle case where notification permission is denied
            android.util.Log.e("GoalNotification", "Permission denied: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            // Handle any other exceptions
            android.util.Log.e("GoalNotification", "Failed to send notification: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Cancel all goal notifications
     * @param context Application context
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_20)
        notificationManager.cancel(NOTIFICATION_ID_50)
        notificationManager.cancel(NOTIFICATION_ID_80)
        notificationManager.cancel(NOTIFICATION_ID_100)
    }

    /**
     * Send a test notification to verify notifications are working
     * Useful for debugging on Android 12+
     * @param context Application context
     */
    fun sendTestNotification(context: Context) {
        android.util.Log.d("GoalNotification", "Sending test notification...")
        android.util.Log.d("GoalNotification", "Android version: ${Build.VERSION.SDK_INT}")

        // Check notification permission
        val notificationManager = NotificationManagerCompat.from(context)
        val areEnabled = notificationManager.areNotificationsEnabled()
        android.util.Log.d("GoalNotification", "Notifications enabled: $areEnabled")

        if (!areEnabled) {
            android.util.Log.w("GoalNotification", "⚠️ Notifications are disabled. Enable them in Settings.")
        }

        sendNotification(
            context,
            9999,
            "Test Notification 🔔",
            "If you see this, notifications are working on Android ${Build.VERSION.SDK_INT}!"
        )
    }
}

