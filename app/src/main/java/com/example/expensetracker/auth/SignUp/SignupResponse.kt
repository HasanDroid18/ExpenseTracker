package com.example.expensetracker.auth.SignUp

import com.example.expensetracker.auth.UserResponse

data class SignupResponse(
    val token: String?,
    val user: UserResponse?,
    val message: String? // this will hold "Email already registered"
)