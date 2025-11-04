package com.example.expensetracker.AppScreens.Settings

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.UserDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val api: ApiService,
    private val userDataStore: UserDataStore
) {

    /**
     * Logout the user and clear all user data (token, username, email)
     */
    suspend fun logout(): Result<String> {
        return try {
            // Step 1: Make API call to logout
            val response = api.logout()

            // Step 2: Check if API call was successful
            if (response.isSuccessful) {
                // Step 3: Clear all user data from local storage
                userDataStore.clearUser()

                // Step 4: Return success message
                val message = response.body()?.message ?: "Logout successful"
                Result.success(message)
            } else {
                // Error - API returned an error code
                Result.failure(Exception("Logout failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "logout failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SettingsRepository"
    }
}

