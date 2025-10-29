package com.example.expensetracker.AppScreens.Home

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.AppScreens.Home.MonthlySummaryResponse
import com.example.expensetracker.auth.TokenDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) {

    /**
     * Get the account summary (balance, income, expenses)
     */
    suspend fun getSummary(): Result<SummaryResponse> {
        return try {
            // Step 1: Get the authentication token
            val token = getToken()

            // Step 2: Make API call with token
            val response = api.getSummary("Bearer $token")

            // Step 3: Check if API call was successful
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Success - return the data
                    Result.success(body)
                } else {
                    // Error - no data received
                    Result.failure(Exception("No summary data received"))
                }
            } else {
                // Error - API returned an error code
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "getSummary failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the monthly report for a specific year and month
     */
    suspend fun getMonthlyReport(year: Int, month: Int): Result<MonthlySummaryResponse> {
        return try {
            // Step 1: Get the authentication token
            val token = getToken()

            // Step 2: Make API call with token
            val response = api.getMonthlyReport("Bearer $token", month, year)

            // Step 3: Check if API call was successful
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Success - return the data
                    Result.success(body)
                } else {
                    // Error - no data received
                    Result.failure(Exception("No monthly report data received"))
                }
            } else {
                // Error - API returned an error code
                Result.failure(Exception("API Error: ${response.code()} for $year-$month"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "getMonthlyReport failed for $year-$month: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function: Get authentication token from storage
     * Throws an exception if token is not available
     */
    private suspend fun getToken(): String {
        val token = tokenDataStore.tokenFlow.first()

        // Check if token exists and is not empty
        if (token == null || token.isEmpty()) {
            throw Exception("No authentication token available. Please login again.")
        }

        return token
    }

    companion object {
        private const val TAG = "HomeRepository"
    }
}

