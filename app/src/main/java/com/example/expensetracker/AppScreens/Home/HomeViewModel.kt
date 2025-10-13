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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: ApiService, @ApplicationContext private val context: Context) : ViewModel(){

    private val _summary = MutableLiveData<SummaryResponse?>()
    val summary: LiveData<SummaryResponse?> get() = _summary

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    // Data caching flags
    private var summaryLoaded = false
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

    private fun shouldRefreshData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) > cacheValidityDuration
    }

    fun loadDataIfNeeded(forceRefresh: Boolean = false) {
        if (forceRefresh || !summaryLoaded || shouldRefreshData()) {
            fetchSummary()
        }
    }

    fun refreshData() {
        summaryLoaded = false
        fetchSummary()
    }

    fun fetchSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val token = TokenDataStore(context).tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.postValue("No token found")
                    _summary.postValue(null)
                    return@launch
                }

                val response = api.getSummary("Bearer $token")
                if (response.isSuccessful) {
                    val summaryData = response.body()
                    if (summaryData != null) {
                        _summary.postValue(summaryData)
                        summaryLoaded = true
                        lastFetchTime = System.currentTimeMillis()
                    } else {
                        _summary.postValue(null)
                        _error.postValue("No summary data received")
                    }
                } else {
                    _error.postValue("Error: ${response.code()} ${response.message()}")
                    _summary.postValue(null)
                }
            } catch (e: Exception) {
                _error.postValue("Network error: ${e.localizedMessage ?: "Unknown error"}")
                _summary.postValue(null)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}