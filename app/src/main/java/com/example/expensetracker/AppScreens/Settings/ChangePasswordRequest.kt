package com.example.expensetracker.AppScreens.Settings

/**
 * Data class for Change Password API request
 *
 * @param oldPassword The user's current password
 * @param newPassword The user's desired new password
 */
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)


