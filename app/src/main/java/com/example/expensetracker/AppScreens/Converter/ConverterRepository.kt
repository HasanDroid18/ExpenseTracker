package com.example.expensetracker.AppScreens.Converter

import android.util.Log
import com.example.expensetracker.Api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConverterRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun fetchExchangeRate(): Result<Double> = runCatching {
        val response = api.getExchangeRate()
        if (response.isSuccessful) {
            val body = response.body() ?: throw Exception("Empty response")
            if (body.success) body.rate else throw Exception("Exchange API returned success = false")
        } else {
            throw Exception("API Error: ${response.code()} - ${response.message()}")
        }
    }.onFailure { e ->
        Log.e(TAG, "Failed to fetch exchange rate: ${e.message}", e)
    }

    companion object {
        private const val TAG = "ConverterRepository"
    }
}

