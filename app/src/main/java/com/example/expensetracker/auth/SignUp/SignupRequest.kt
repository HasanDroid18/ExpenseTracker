package com.example.expensetracker.auth.SignUp

data class SignupRequest(
    val email: String,
    val username: String,
    val password: String
)