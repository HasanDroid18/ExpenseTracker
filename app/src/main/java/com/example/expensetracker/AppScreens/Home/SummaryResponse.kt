package com.example.expensetracker.AppScreens.Home

import com.google.gson.annotations.SerializedName

data class SummaryResponse(
    @SerializedName("balance")
    val balance: String = "$0.00",
    @SerializedName("expenses")
    val expenses: String = "$0.00",
    @SerializedName("income")
    val income: String = "$0.00"
)