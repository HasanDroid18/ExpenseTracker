package com.example.expensetracker.AppScreens.Settings

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.UserDataStore
import kotlinx.coroutines.flow.first
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

    /**
     * Change user's password
     *
     * @param oldPassword The user's current password
     * @param newPassword The user's desired new password
     * @return Result with success message or error
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> {
        return try {
            // Step 1: Get the authentication token from UserDataStore
            val token = userDataStore.tokenFlow.first()

            if (token == null) {
                return Result.failure(
                    Exception("Authentication required. Please login again.")
                )
            }

            // Step 2: Create request body
            val request = ChangePasswordRequest(oldPassword, newPassword)

            // Step 3: Make API call to change password with token
            val response = api.changePassword("Bearer $token", request)

            // Step 4: Check if API call was successful
            if (response.isSuccessful) {
                // Success - return the message from backend
                val message = response.body()?.message ?: "Password changed successfully"
                Log.d(TAG, "Password changed successfully")
                Result.success(message)
            } else {
                // Error - parse error message from backend
                val errorMessage = try {
                    // Try to parse error message from response body
                    val errorBody = response.errorBody()?.string()
                    // Extract message from JSON (simple parsing)
                    errorBody?.let {
                        val messageStart = it.indexOf("\"message\":\"") + 11
                        val messageEnd = it.indexOf("\"", messageStart)
                        if (messageStart > 10 && messageEnd > messageStart) {
                            it.substring(messageStart, messageEnd)
                        } else {
                            "Failed to change password"
                        }
                    } ?: "Failed to change password"
                } catch (e: Exception) {
                    "Failed to change password: ${response.code()}"
                }

                Log.e(TAG, "changePassword failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            // Network error or other exception
            Log.e(TAG, "changePassword exception: ${e.message}", e)
            Result.failure(Exception("Network error. Please check your connection."))
        }
    }

    companion object {
        private const val TAG = "SettingsRepository"
    }
}

