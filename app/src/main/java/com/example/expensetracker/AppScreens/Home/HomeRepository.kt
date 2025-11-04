package com.example.expensetracker.AppScreens.Home

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.UserDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: ApiService,
    private val userDataStore: UserDataStore
) {

    /**
     * Get username from local storage
     */
    suspend fun getUsername(): Result<String> {
        return try {
            val username = userDataStore.usernameFlow.first()
            if (username != null && username.isNotEmpty()) {
                Result.success(username)
            } else {
                Result.failure(Exception("Username not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUsername failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the account summary including total balance, income, and expenses
     */
    suspend fun getSummary(): Result<SummaryResponse> = runCatching {
        val token = getAuthToken()
        val response = api.getSummary("Bearer $token")
        handleApiResponse(response)
    }.onFailure { error ->
        Log.e(TAG, "Failed to fetch summary: ${error.message}", error)
    }

    /**
     * Fetches the monthly report for a specific year and month
     */
    suspend fun getMonthlyReport(year: Int, month: Int): Result<MonthlySummaryResponse> = runCatching {
        val token = getAuthToken()
        val response = api.getMonthlyReport("Bearer $token", month, year)
        handleApiResponse(response)
    }.onFailure { error ->
        Log.e(TAG, "Failed to fetch monthly report ($year-$month): ${error.message}", error)
    }

    /**
     * Retrieves the authentication token from the data store
     */
    private suspend fun getAuthToken(): String {
        val token = userDataStore.tokenFlow.first()
        return token?.takeIf { it.isNotEmpty() }
            ?: throw Exception("Authentication token not available")
    }

    /**
     * Handles API responses uniformly
     */
    private inline fun <reified T> handleApiResponse(response: retrofit2.Response<T>): T {
        return when {
            response.isSuccessful -> {
                response.body() ?: throw Exception("Empty response body")
            }
            else -> {
                throw Exception("API Error: ${response.code()} - ${response.message()}")
            }
        }
    }

    companion object {
        private const val TAG = "HomeRepository"
    }
}
