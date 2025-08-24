package com.example.expensetracker.Api

import com.example.expensetracker.AppScreens.Home.SummaryResponse
import com.example.expensetracker.AppScreens.Home.TransactionResponse
import com.example.expensetracker.auth.Login.LoginRequest
import com.example.expensetracker.auth.Login.LoginResponse
import com.example.expensetracker.AppScreens.Settings.LogoutResponse
import com.example.expensetracker.auth.SignUp.SignupRequest
import com.example.expensetracker.auth.SignUp.SignupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    @GET("transactions/me/transactions")
    suspend fun getTransactions(@Header("Authorization") token: String): Response<List<TransactionResponse>>

    @GET("transactions/me/summary")
    suspend fun getSummary(@Header("Authorization") token: String): Response<SummaryResponse>
}