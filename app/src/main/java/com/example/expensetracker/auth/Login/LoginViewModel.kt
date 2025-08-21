package com.example.expensetracker.auth.Login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor( private val api: ApiService): ViewModel() {
    private val _loginResponse = MutableLiveData<Result<LoginResponse>>()
    val loginResponse: LiveData<Result<LoginResponse>> = _loginResponse

    fun login(email:String,password:String){
        viewModelScope.launch {
            try{
                val response =api.login(LoginRequest(email,password))
                if (response.isSuccessful){
                    response.body()?.let {
                        _loginResponse.postValue(Result.success(it))
                    }?:run {
                        _loginResponse.postValue(Result.failure(Exception("Empty response body")))
                    }
                }else{
                    val errorBody = response.errorBody()?.string()
                    _loginResponse.postValue(Result.failure(Exception(errorBody)))
                }
            }catch (e:Exception){
                _loginResponse.postValue(Result.failure(e))
            }
        }
    }
}