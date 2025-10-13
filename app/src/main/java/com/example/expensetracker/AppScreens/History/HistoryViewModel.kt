package com.example.expensetracker.AppScreens.History

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.AppScreens.Home.TransactionResponse
import com.example.expensetracker.auth.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _transactions = MutableLiveData<List<TransactionResponse>>(emptyList())
    val transactions: LiveData<List<TransactionResponse>> get() = _transactions

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private var transactionsLoaded = false
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

    private fun shouldRefreshData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) > cacheValidityDuration
    }

    fun loadDataIfNeeded(forceRefresh: Boolean = false) {
        if (forceRefresh || !transactionsLoaded || shouldRefreshData()) {
            fetchTransactions()
        }
    }

    fun fetchTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val token = TokenDataStore(context).tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.postValue("No token found")
                    _transactions.postValue(emptyList())
                    return@launch
                }

                val response = api.getTransactions("Bearer $token")
                if (response.isSuccessful) {
                    val list = response.body().orEmpty()
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val sortedList = list.sortedByDescending { txn ->
                        try { parser.parse(txn.created_at) ?: Date(0) } catch (_: Exception) { Date(0) }
                    }
                    _transactions.postValue(sortedList)
                    transactionsLoaded = true
                    lastFetchTime = System.currentTimeMillis()
                    _error.postValue("")
                } else {
                    _error.postValue("Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Unknown error")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val token = TokenDataStore(context).tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.postValue("No token found")
                    return@launch
                }
                val response = api.deleteTransaction("Bearer $token", id.toString())
                if (response.isSuccessful) {
                    transactionsLoaded = false
                    fetchTransactions()
                } else {
                    _error.postValue("Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Unknown error")
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
