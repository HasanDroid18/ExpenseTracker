package com.example.expensetracker.auth

import android.util.Log
import com.example.expensetracker.Api.ApiService
import com.example.expensetracker.auth.Login.LoginRequest
import com.example.expensetracker.auth.Login.LoginResponse
import com.example.expensetracker.auth.SignUp.SignupRequest
import com.example.expensetracker.auth.SignUp.SignupResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val userDataStore: UserDataStore
) {

    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            // Step 1: Make API call with login credentials
            val response = api.login(LoginRequest(email, password))

            // Step 2: Check if API call was successful
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Step 3: Save token to local storage
                    tokenDataStore.saveToken(body.token)

                    // Step 4: Save username and email to local storage
                    userDataStore.saveUser(body.user.username, body.user.email)

                    // Step 5: Return success with login response
                    Result.success(body)
                } else {
                    // Error - no data received
                    Result.failure(Exception("No login data received"))
                }
            } else {
                // Error - API returned an error code
                Result.failure(Exception("Login failed (${response.code()})"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Register a new user with email, username, and password
     */
    suspend fun signup(email: String, username: String, password: String): Result<SignupResponse> {
        return try {
            // Step 1: Make API call with signup credentials
            val response = api.signup(SignupRequest(email, username, password))

            // Step 2: Check if API call was successful
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Step 3: Save username to local storage
                    userDataStore.saveUsername(username)

                    // Step 4: Return success with signup response
                    Result.success(body)
                } else {
                    // Error - no data received
                    Result.failure(Exception("No signup data received"))
                }
            } else {
                // Error - API returned an error code
                Result.failure(Exception("Signup failed (${response.code()})"))
            }
        } catch (e: Exception) {
            // Error - log it and return failure
            Log.e(TAG, "signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
