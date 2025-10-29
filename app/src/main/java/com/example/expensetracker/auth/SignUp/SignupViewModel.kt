package com.example.expensetracker.auth.SignUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _signupResponse = MutableLiveData<Result<SignupResponse>>()
    val signupResponse: LiveData<Result<SignupResponse>> = _signupResponse

    /**
     * Register a new user with email, username, and password
     */
    fun signup(email: String, username: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signup(email, username, password)
            _signupResponse.value = result
        }
    }
}