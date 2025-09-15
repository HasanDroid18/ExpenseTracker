package com.example.expensetracker.AppScreens

data class TransactionRequest(
    val amount: Int,
    val category: String,
    val title: String
)