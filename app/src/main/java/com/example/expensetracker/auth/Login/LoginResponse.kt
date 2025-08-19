package com.example.expensetracker.auth.Login

import com.example.expensetracker.auth.UserResponse

// LoginResponse.kt
data class LoginResponse(
    val token: String,
    val user: UserResponse
)