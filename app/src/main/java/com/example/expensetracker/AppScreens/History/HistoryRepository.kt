package com.example.expensetracker.AppScreens.History

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.UserDataStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val api: ApiService,
    private val userDataStore: UserDataStore
) {

    /**
     * Fetches all transactions from the API and sorts them by date (newest first)
     */
    suspend fun getTransactions(): Result<List<TransactionResponse>> {
        return try {
            // Step 1: Get authentication token
            val token = getToken()

            // Step 2: Make API call to get transactions
            val response = api.getTransactions("Bearer $token")

            // Step 3: Check if API call was successful
            if (response.isSuccessful) {
                val transactionList = response.body().orEmpty()

                // Step 4: Sort transactions by date (newest first)
                val sortedList = sortTransactionsByDate(transactionList)

                // Step 5: Return success
                Result.success(sortedList)
            } else {
                // Error - API returned an error code
                Result.failure(Exception("Failed to fetch transactions (${response.code()})"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "getTransactions failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a transaction by its ID
     */
    suspend fun deleteTransaction(transactionId: Int): Result<String> {
        return try {
            // Step 1: Get authentication token
            val token = getToken()

            // Step 2: Make API call to delete transaction
            val response = api.deleteTransaction("Bearer $token", transactionId.toString())

            // Step 3: Check if API call was successful
            if (response.isSuccessful) {
                // Step 4: Return success message
                Result.success("Transaction deleted successfully")
            } else {
                // Error - API returned an error code
                Result.failure(Exception("Failed to delete transaction (${response.code()})"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "deleteTransaction failed for ID $transactionId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function: Get authentication token from storage
     * Throws an exception if token is not available
     */
    private suspend fun getToken(): String {
        val token = userDataStore.tokenFlow.first()

        // Check if token exists and is not empty
        if (token == null || token.isEmpty()) {
            throw Exception("No authentication token available. Please login again.")
        }

        return token
    }

    /**
     * Helper function: Sort transactions by creation date (newest first)
     */
    private fun sortTransactionsByDate(transactions: List<TransactionResponse>): List<TransactionResponse> {
        // Create date formatter for parsing ISO 8601 format
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Sort by date descending (newest first)
        return transactions.sortedByDescending { transaction ->
            try {
                parser.parse(transaction.created_at) ?: Date(0)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse date for transaction: ${transaction.created_at}")
                Date(0)
            }
        }
    }

    companion object {
        private const val TAG = "HistoryRepository"
    }
}

