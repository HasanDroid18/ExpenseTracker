package com.example.expensetracker.AppScreens.Home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: ApiService, @ApplicationContext private val context: Context) : ViewModel(){
    private val _transactions = MutableLiveData<List<TransactionResponse>>()
    val transactions: LiveData<List<TransactionResponse>> get() = _transactions

    private val _summary = MutableLiveData<SummaryResponse?>()
    val summary: LiveData<SummaryResponse?> get() = _summary

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> get() = _loading

    // Track multiple parallel loads (transactions + summary)
    @Volatile private var activeRequests = 0
    private fun beginLoad() {
        activeRequests += 1
        _loading.value = activeRequests > 0
    }
    private fun endLoad() {
        if (activeRequests > 0) activeRequests -= 1
        _loading.value = activeRequests > 0
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            beginLoad()
            try {
                val tokenDataStore = TokenDataStore(context)
                val token = tokenDataStore.tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.value = "No token found"
                    return@launch
                }

                val response = api.getTransactions("Bearer $token")
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()

                    // Sort by created_at descending (newest first)
                    val sortedList = list.sortedByDescending { transaction ->
                        try {
                            val parser = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.getDefault()
                            )
                            parser.timeZone = TimeZone.getTimeZone("UTC")
                            parser.parse(transaction.created_at) ?: Date(0)
                        } catch (_: Exception) {
                            Date(0)
                        }
                    }

                    _transactions.value = sortedList
                } else {
                    _error.value = "Error: ${response.code()} ${response.message()}"
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                endLoad()
            }
        }
    }

    fun fetchSummary() {
        viewModelScope.launch {
            beginLoad()
            try {
                val tokenDataStore = TokenDataStore(context)
                val token = tokenDataStore.tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.value = "No token found"
                    return@launch
                }

                val response = api.getSummary("Bearer $token")
                if (response.isSuccessful) {
                    val summaryData = response.body()
                    if (summaryData != null) {
                        _summary.value = summaryData
                    } else {
                        // Handle null response body
                        _summary.value = SummaryResponse()
                        _error.value = "No summary data received"
                    }
                } else {
                    _error.value = "Error: ${response.code()} ${response.message()}"
                    // Set default values on error
                    _summary.value = SummaryResponse()
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                // Set default values on exception
                _summary.value = SummaryResponse()
            } finally {
                endLoad()
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            beginLoad()
            try {
                val tokenDataStore = TokenDataStore(context)
                val token = tokenDataStore.tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.value = "No token found"
                    return@launch
                }
                val response = api.deleteTransaction("Bearer $token", id.toString())
                if (response.isSuccessful) {
                    // Refresh data after deletion
                    fetchTransactions()
                    fetchSummary()
                } else {
                    _error.value = "Error: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                endLoad()
            }
        }
    }
}