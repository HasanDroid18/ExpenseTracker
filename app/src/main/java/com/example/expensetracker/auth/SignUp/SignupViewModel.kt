package com.example.expensetracker.auth.SignUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Api.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(private val api: ApiService) : ViewModel() {

    private val _signupResponse = MutableLiveData<Result<SignupResponse>>()
    val signupResponse: LiveData<Result<SignupResponse>> = _signupResponse

    fun signup(email: String, username: String, password: String){
        viewModelScope.launch {
            try {
                val response = api.signup(SignupRequest(email,username,password))
                if (response.isSuccessful){
                    response.body()?.let {
                        _signupResponse.postValue(Result.success(it))
                    }?:run{
                        _signupResponse.postValue(Result.failure(Exception("Empty response body")))
                    }
                }else{
                    val errorBody=response.errorBody()?.string()
                    _signupResponse.postValue(Result.failure(Exception(errorBody)))

                }
            }catch (e:(Exception)){
                _signupResponse.postValue(Result.failure(e))
            }
        }
    }
}