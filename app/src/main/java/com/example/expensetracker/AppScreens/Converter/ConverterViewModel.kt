package com.example.expensetracker.AppScreens.Converter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val repository: ConverterRepository
) : ViewModel() {
    private val _result = MutableLiveData<String>()
    val result: LiveData<String> get() = _result

    private val _rate = MutableLiveData<Double>()
    val rate: LiveData<Double> get() = _rate

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    init {
        refreshRate()
    }

    fun refreshRate() {
        viewModelScope.launch {
            _loading.postValue(true)
            _error.postValue(null)
            val res = repository.fetchExchangeRate()
            res.onSuccess { value ->
                _rate.postValue(value)
            }.onFailure { e ->
                _error.postValue(e.message ?: "Failed to fetch exchange rate")
            }
            _loading.postValue(false)
        }
    }

    fun convert(amount: Double, modeIndex: Int) {
        val exchangeRate = _rate.value
        if (exchangeRate == null) {
            _error.value = "Exchange rate not loaded"
            return
        }
        val converted = when (modeIndex) {
            0 -> amount * exchangeRate  // USD to LBP
            1 -> if (exchangeRate != 0.0) amount / exchangeRate else 0.0  // LBP to USD
            else -> 0.0
        }
        _result.value = String.format(Locale.US, "%.2f", converted)
    }
}
