package com.example.expensetracker.AppScreens.Converter

import com.google.gson.annotations.SerializedName

// Example response:
// {"base":"USD","target":"LBP","rate":89500.5,"last_updated":"2025-11-05T15:30:00Z","success":true}

data class ExchangeResponse(
    val base: String,
    val target: String,
    val rate: Double,
    @SerializedName("last_updated") val lastUpdated: String,
    val success: Boolean
)

