package com.example.expensetracker.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TokenDataStore(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore("user_prefs")
        val USER_TOKEN_KEY = stringPreferencesKey("USER_TOKEN")
    }

    // Save token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_TOKEN_KEY] = token
        }
    }

    // Clear token (logout)
    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_TOKEN_KEY)
        }
    }

    // Read token
    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_TOKEN_KEY]
    }
}