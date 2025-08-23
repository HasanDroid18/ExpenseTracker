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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: ApiService, @ApplicationContext private val context: Context) : ViewModel(){

    // LiveData to notify UI about logout success or failure
    private val _logoutState = MutableLiveData<Result<String>>()
    val logoutState: LiveData<Result<String>> = _logoutState

    fun logout(){
        viewModelScope.launch {
            try {
                val response =api.logout()
                if (response.isSuccessful){
                    val tokenDataStore = TokenDataStore(context)
                    tokenDataStore.clearToken()
                    _logoutState.value = Result.success(
                        response.body()?.message ?: "Logout successful"
                    )
                }else{
                    //Handle unsuccessful response
                    _logoutState.value = Result.failure(
                        Exception("Logout failed: ${response.errorBody()?.string()}" )
                    )
                }
            }catch(e:Exception){
                _logoutState.value = Result.failure(e)
            }
        }
    }
}