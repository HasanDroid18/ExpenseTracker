package com.example.expensetracker.auth.biometric

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * AppLockLifecycleObserver monitors the app's lifecycle to trigger authentication
 * when the app comes to the foreground.
 *
 * This observer:
 * - Detects when the app moves from background to foreground
 * - Triggers authentication callback when needed
 * - Prevents redundant authentication requests
 */
class AppLockLifecycleObserver(
    private val onAuthenticationRequired: () -> Unit
) : DefaultLifecycleObserver {

    private var isAppInBackground = false
    private var hasAuthenticatedThisSession = false

    /**
     * Called when the app process starts or comes to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        // If app was in background and hasn't authenticated this session, require auth
        if (isAppInBackground || !hasAuthenticatedThisSession) {
            onAuthenticationRequired()
        }

        isAppInBackground = false
    }

    /**
     * Called when the app process goes to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInBackground = true
        // Reset authentication flag so it's required again when app returns
        hasAuthenticatedThisSession = false
    }

    /**
     * Mark that user has successfully authenticated in this session
     */
    fun markAuthenticated() {
        hasAuthenticatedThisSession = true
    }

    /**
     * Force authentication to be required next time
     */
    fun requireReAuthentication() {
        hasAuthenticatedThisSession = false
    }
}

