package com.example.expensetracker.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserDataStore(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore("user_data")
        val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
        val USER_EMAIL_KEY = stringPreferencesKey("USER_EMAIL")
    }

    // Save user data
    suspend fun saveUser(username: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = username
            prefs[USER_EMAIL_KEY] = email
        }
    }

    // Save username only
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = username
        }
    }

    // Read username
    val usernameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    // Read email
    val emailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    // Clear user data (logout)
    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
        }
    }
}

