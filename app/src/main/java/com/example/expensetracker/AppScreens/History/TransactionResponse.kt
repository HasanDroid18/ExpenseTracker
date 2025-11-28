package com.example.expensetracker.AppScreens.History

data class TransactionResponse(
    val amount: Double,
    val category: String,
    val created_at: String,
    val id: Int,
    val title: String,
    val user_id: String,
    val date: String,  // Format: "YYYY-MM-DD"
    val type: String  // "income" or "expense"

)