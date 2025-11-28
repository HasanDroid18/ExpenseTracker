package com.example.expensetracker.AppScreens.Settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.auth.UserDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userDataStore: UserDataStore
) : ViewModel() {

    // LiveData to notify UI about logout success or failure
    private val _logoutState = MutableLiveData<Result<String>>()
    val logoutState: LiveData<Result<String>> = _logoutState

    // LiveData to notify UI about change password success or failure
    private val _changePasswordState = MutableLiveData<Result<String>>()
    val changePasswordState: LiveData<Result<String>> = _changePasswordState

    // LiveData to control loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Expose username and email from DataStore
    val username: LiveData<String?> = userDataStore.usernameFlow.asLiveData()
    val email: LiveData<String?> = userDataStore.emailFlow.asLiveData()

    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            val result = settingsRepository.logout()
            _logoutState.value = result
        }
    }

    /**
     * Change user's password
     *
     * @param oldPassword The user's current password
     * @param newPassword The user's desired new password
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            // Set loading state
            _isLoading.value = true

            // Call repository to change password
            val result = settingsRepository.changePassword(oldPassword, newPassword)

            // Update state with result
            _changePasswordState.value = result

            // Reset loading state
            _isLoading.value = false
        }
    }
}