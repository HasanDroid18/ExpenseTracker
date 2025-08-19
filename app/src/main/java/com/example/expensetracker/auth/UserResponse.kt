package com.example.expensetracker.auth

data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val created_at: String? = null
)


