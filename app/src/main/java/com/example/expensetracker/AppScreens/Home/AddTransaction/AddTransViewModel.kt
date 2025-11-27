package com.example.expensetracker.AppScreens.Home.AddTransaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.AppScreens.History.TransactionResponse
import com.example.expensetracker.auth.UserDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class AddTransViewModel @Inject constructor(
    private val api: ApiService,
    private val userDataStore: UserDataStore
) : ViewModel(){
    private val _addTransactionResponse = MutableLiveData<Result<TransactionResponse>>()
    val addTransactionResponse: MutableLiveData<Result<TransactionResponse>> = _addTransactionResponse

    fun addTransaction(request: TransactionRequest) {
        viewModelScope.launch {
            try {
                val token = userDataStore.tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _addTransactionResponse.postValue(Result.failure(Exception("No token found")))
                    return@launch
                }

                // Validate balance for expense transactions
                if (request.category.lowercase() == "expense") {
                    val summaryResponse = api.getSummary("Bearer $token")
                    if (summaryResponse.isSuccessful) {
                        val balanceString = summaryResponse.body()?.balance ?: "$0.00"
                        // Parse balance by removing $ and +/- signs, then converting to Double
                        val cleanBalance = balanceString.replace(Regex("[^0-9.]"), "")
                        val currentBalance: Double = cleanBalance.toDoubleOrNull() ?: 0.0

                        if (request.amount > currentBalance) {
                            _addTransactionResponse.postValue(
                                Result.failure(Exception("Insufficient balance. Current balance: $balanceString"))
                            )
                            return@launch
                        }
                    }
                }

                val response = api.createTransaction("Bearer $token", request)
                if (response.isSuccessful){
                    response.body()?.let {
                        _addTransactionResponse.postValue(Result.success(it))
                    }?: run {
                        _addTransactionResponse.postValue(Result.failure(Exception("Empty response body")))
                    }
                }else{
                    val errorBody = response.errorBody()?.string()
                    _addTransactionResponse.postValue(Result.failure(Exception(errorBody)))
                }
            }catch (e: Exception){
                _addTransactionResponse.postValue(Result.failure(e))
            }
        }

    }
}