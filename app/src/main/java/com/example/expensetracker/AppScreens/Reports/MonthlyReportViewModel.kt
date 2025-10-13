package com.example.expensetracker.AppScreens.Reports

import android.content.Context
import android.util.Log
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
class MonthlyReportViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _reports = MutableLiveData<List<MonthlySummaryResponse>>(emptyList())
    val reports: LiveData<List<MonthlySummaryResponse>> get() = _reports

    private val _monthlySummary = MutableLiveData<MonthlySummaryResponse?>(null)
    val monthlySummary: LiveData<MonthlySummaryResponse?> get() = _monthlySummary

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    fun loadMonthlyReport(year: Int, month: Int) {
        // Run the whole fetch on IO to avoid main-thread pressure
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                Log.d("MonthlyReportVM", "Fetching monthly report for $year-$month")
                val token = TokenDataStore(context).tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _error.postValue("No token found")
                    _reports.postValue(emptyList())
                    _monthlySummary.postValue(null)
                    Log.w("MonthlyReportVM", "No token found; aborting request")
                    return@launch
                }

                // Single API call (no duplicate fallback)
                val res = api.getMonthlyReport("Bearer $token", month, year)
                Log.d("MonthlyReportVM", "code=${res.code()} success=${res.isSuccessful}")
                if (res.isSuccessful) {
                    val summary = res.body()
                    if (summary == null) {
                        _error.postValue("No data for selected month")
                        _monthlySummary.postValue(null)
                    } else {
                        _error.postValue(null)
                        _monthlySummary.postValue(summary)
                    }
                } else {
                    val msg = "Error: ${res.code()} ${res.message()}"
                    Log.e("MonthlyReportVM", msg)
                    _error.postValue(msg)
                    _monthlySummary.postValue(null)
                }

                // Clear daily list until a daily endpoint is (re)available
                _reports.postValue(emptyList())
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Unknown error")
                _reports.postValue(emptyList())
                _monthlySummary.postValue(null)
                Log.e("MonthlyReportVM", "Exception during fetch", e)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
