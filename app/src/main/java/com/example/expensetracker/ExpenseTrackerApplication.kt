package com.example.expensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp tells Hilt to start dependency injection for the whole app
// It generates code that hooks into the Application lifecycle
@HiltAndroidApp
class ExpenseTrackerApplication: Application() {
    // This class can be used to initialize global state or libraries
    // For example, you can initialize a logging library or a database here
    // Currently, it does not have any specific initialization logic
}