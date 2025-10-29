package com.example.expensetracker.AppScreens.Home.AddTransaction

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.AppScreens.History.TransactionResponse
import com.example.expensetracker.auth.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class AddTransViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel(){
    private val _addTransactionResponse = MutableLiveData<Result<TransactionResponse>>()
    val addTransactionResponse: MutableLiveData<Result<TransactionResponse>> = _addTransactionResponse

    fun addTransaction(request: TransactionRequest) {
        viewModelScope.launch {
            try {
                val tokenDataStore = TokenDataStore(context)
                val token = tokenDataStore.tokenFlow.first()
                if (token.isNullOrEmpty()) {
                    _addTransactionResponse.postValue(Result.failure(Exception("No token found")))
                    return@launch
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