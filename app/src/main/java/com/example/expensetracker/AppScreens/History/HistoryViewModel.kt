package com.example.expensetracker.AppScreens.History

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _transactions = MutableLiveData<List<TransactionResponse>>(emptyList())
    val transactions: LiveData<List<TransactionResponse>> get() = _transactions

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    // Data caching flags
    private var transactionsLoaded = false
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

    private fun shouldRefreshData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) > cacheValidityDuration
    }

    /**
     * Load transactions if needed (respects cache)
     */
    fun loadDataIfNeeded(forceRefresh: Boolean = false) {
        if (forceRefresh || !transactionsLoaded || shouldRefreshData()) {
            fetchTransactions()
        }
    }

    /**
     * Fetch transactions from repository
     */
    private fun fetchTransactions() {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue(null) // Clear previous errors

            val result = historyRepository.getTransactions()

            result.onSuccess { transactionList ->
                _transactions.postValue(transactionList)
                transactionsLoaded = true
                lastFetchTime = System.currentTimeMillis()
            }.onFailure { error ->
                _error.postValue(error.message ?: "Failed to load transactions")
                _transactions.postValue(emptyList())
            }

            _loading.postValue(false)
        }
    }

    /**
     * Delete a transaction by ID and refresh the list
     */
    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue(null) // Clear previous errors

            val result = historyRepository.deleteTransaction(transactionId)

            result.onSuccess {
                // Refresh the transactions list after successful deletion
                transactionsLoaded = false
                fetchTransactions()
            }.onFailure { error ->
                _error.postValue(error.message ?: "Failed to delete transaction")
                _loading.postValue(false)
            }
        }
    }
}
