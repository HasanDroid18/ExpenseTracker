package com.example.expensetracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.expensetracker.AppScreens.Goals.GoalNotificationBuilder
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp tells Hilt to start dependency injection for the whole app
// It generates code that hooks into the Application lifecycle
@HiltAndroidApp
class ExpenseTrackerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // Force light mode - disable dark mode completely
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize notification channel for goal notifications
        GoalNotificationBuilder.createNotificationChannel(this)
    }
}