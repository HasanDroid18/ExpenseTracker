package com.example.expensetracker.AppScreens.Home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val homeRepository: HomeRepository) : ViewModel() {

    private val _summary = MutableLiveData<SummaryResponse?>()
    val summary: LiveData<SummaryResponse?> get() = _summary

    private val _monthlySummary = MutableLiveData<MonthlySummaryResponse?>()
    val monthlySummary: LiveData<MonthlySummaryResponse?> get() = _monthlySummary

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    // Data caching flags
    private var dataLoaded = false
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

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
            _error.postValue(null) // Clear previous errors

            try {
                // Execute API calls concurrently
                val usernameDeferred = async { homeRepository.getUsername() as Result<String> }
                val summaryDeferred = async { homeRepository.getSummary() as Result<SummaryResponse> }
                val reportDeferred = async {
                    val cal = Calendar.getInstance()
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1
                    homeRepository.getMonthlyReport(year, month) as Result<MonthlySummaryResponse>
                }

                val usernameResult = usernameDeferred.await()
                val summaryResult = summaryDeferred.await()
                val reportResult = reportDeferred.await()

                // Handle username result
                (usernameResult as Result<String>).onSuccess { usernameData ->
                    _username.postValue(usernameData)
                }.onFailure { error ->
                    _username.postValue("User") // Fallback username
                }

                // Handle summary result
                (summaryResult as Result<SummaryResponse>).onSuccess { summaryData ->
                    _summary.postValue(summaryData)
                }.onFailure { error ->
                    _error.postValue(error.message ?: "Failed to load summary")
                    _summary.postValue(null)
                }

                // Handle monthly report result
                (reportResult as Result<MonthlySummaryResponse>).onSuccess { monthlyData ->
                    _monthlySummary.postValue(monthlyData)
                }.onFailure { error ->
                    if (_error.value == null) { // Only show error if no previous error
                        _error.postValue(error.message ?: "Failed to load monthly report")
                    }
                    _monthlySummary.postValue(null)
                }

                dataLoaded = true
                lastFetchTime = System.currentTimeMillis()
            } finally {
                _loading.postValue(false)
            }
        }
    }
}