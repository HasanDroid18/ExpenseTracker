package com.example.expensetracker.AppScreens.Home.AddTransaction

data class TransactionRequest(
    val amount: Double,
    val category: String,
    val title: String
)