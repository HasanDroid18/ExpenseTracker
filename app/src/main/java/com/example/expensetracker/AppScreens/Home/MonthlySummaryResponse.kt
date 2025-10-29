package com.example.expensetracker.AppScreens.Home

data class MonthlySummaryResponse(
    val year: Int,
    val month: Int,
    val income: Double?,
    val expenses: Double?,
    val balance: Double?
)