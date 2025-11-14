package com.example.expensetracker.AppScreens.Home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val homeRepository: HomeRepository) : ViewModel() {

    private val _summary = MutableLiveData<SummaryResponse?>()
    val summary: LiveData<SummaryResponse?> get() = _summary

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    // Simple caching flags (5 minutes)
    private var dataLoaded = false
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L

    private fun shouldRefreshData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) > cacheValidityDuration
    }

    fun loadDataIfNeeded(forceRefresh: Boolean = false) {
        if (forceRefresh || !dataLoaded || shouldRefreshData()) {
            fetchAllData()
        }
    }

    fun refreshData() {
        fetchAllData()
    }

    private fun fetchAllData() {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue(null)
            try {
                // Fetch username and summary sequentially for simplicity
                homeRepository.getUsername().onSuccess { name ->
                    _username.postValue(name)
                }.onFailure {
                    _username.postValue("User")
                }

                homeRepository.getSummary().onSuccess { summaryData ->
                    _summary.postValue(summaryData)
                }.onFailure { err ->
                    _summary.postValue(null)
                    _error.postValue(err.message ?: "Failed to load summary")
                }

                dataLoaded = true
                lastFetchTime = System.currentTimeMillis()
            } finally {
                _loading.postValue(false)
            }
        }
    }
}