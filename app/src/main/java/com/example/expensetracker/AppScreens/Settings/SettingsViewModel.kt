package com.example.expensetracker.AppScreens.Settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // LiveData to notify UI about logout success or failure
    private val _logoutState = MutableLiveData<Result<String>>()
    val logoutState: LiveData<Result<String>> = _logoutState

    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            val result = settingsRepository.logout()
            _logoutState.value = result
        }
    }
}