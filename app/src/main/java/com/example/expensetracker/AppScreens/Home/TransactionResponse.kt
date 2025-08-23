package com.example.expensetracker.AppScreens.Home

data class TransactionResponse(
    val amount: String,
    val category: String,
    val created_at: String,
    val id: Int,
    val title: String,
    val user_id: String
)