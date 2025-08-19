package com.example.expensetracker.Api

import com.example.expensetracker.auth.Login.LoginRequest
import com.example.expensetracker.auth.Login.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login") // adjust endpoint
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}