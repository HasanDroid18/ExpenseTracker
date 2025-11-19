package com.example.expensetracker.auth

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.auth.biometric.BiometricAuthManager
import com.example.expensetracker.auth.biometric.BiometricPreferenceManager
import com.example.expensetracker.auth.biometric.BiometricStatus
import com.example.expensetracker.auth.biometric.PinLockActivity
import com.example.expensetracker.auth.biometric.PinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SplashScreen : AppCompatActivity() {
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var pinManager: PinManager
    private lateinit var biometricPreferenceManager: BiometricPreferenceManager
    private var isAuthenticated = false
    private var isAuthenticationInProgress = false
    private var pendingNavigation: Intent? = null

    // Activity result launcher for PIN authentication
    private val pinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticationInProgress = false
        if (result.resultCode == PinLockActivity.RESULT_AUTHENTICATED) {
            onAuthenticationSuccess()
        } else {
            // User cancelled or failed PIN - close app
            onAuthenticationFailure("Authentication required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved language before setting content view
        applySavedLanguage()

        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize authentication managers
        biometricAuthManager = BiometricAuthManager(this)
        pinManager = PinManager(this)
        biometricPreferenceManager = BiometricPreferenceManager(this)

        val userDataStore = UserDataStore(this)

        lifecycleScope.launch {
            // wait 3 seconds for splash
            delay(3000)

            userDataStore.tokenFlow.collect { token ->
                if (!token.isNullOrEmpty()) {
                    // User already logged in
                    pendingNavigation = Intent(this@SplashScreen, MainActivity::class.java)

                    // Check if biometric is enabled in settings
                    if (biometricPreferenceManager.isBiometricEnabled()) {
                        // Require authentication before navigating
                        requestAuthentication()
                    } else {
                        // Biometric disabled, go directly to main
                        navigateToPendingDestination()
                    }
                } else {
                    // Not logged in, go to Login screen (no auth needed)
                    startActivity(Intent(this@SplashScreen, AuthActivity::class.java))
                    finish()
                }
                return@collect // stop collecting after first navigation
            }
        }
    }

    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("selected_language", null)

        val languageCode = when (savedLanguage) {
            "Arabic" -> "ar"
            "English" -> "en"
            else -> null
        }

        if (languageCode != null) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Ensure the layout direction stays Left-to-Right (LTR)
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
    }

    /**
     * Request authentication - tries biometric first, falls back to PIN if unavailable
     */
    private fun requestAuthentication() {
        if (isAuthenticationInProgress) return

        isAuthenticationInProgress = true

        // Check biometric availability
        val biometricStatus = biometricAuthManager.checkBiometricAvailability()

        when (biometricStatus) {
            BiometricStatus.AVAILABLE -> {
                // Biometric authentication is available, show prompt
                showBiometricPrompt()
            }
            BiometricStatus.NONE_ENROLLED -> {
                // No biometrics enrolled - use PIN fallback
                showBiometricNotEnrolledDialog()
            }
            BiometricStatus.NO_HARDWARE,
            BiometricStatus.HARDWARE_UNAVAILABLE,
            BiometricStatus.UNSUPPORTED -> {
                // Biometric not available - use PIN fallback
                showPinFallback()
            }
            else -> {
                // Unknown error - use PIN fallback
                showPinFallback()
            }
        }
    }

    /**
     * Show biometric authentication prompt
     */
    private fun showBiometricPrompt() {
        biometricAuthManager.authenticate(
            onSuccess = {
                // Authentication successful
                onAuthenticationSuccess()
            },
            onError = { errorCode, errorMessage ->
                // Handle authentication errors
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        // User cancelled - close app
                        onAuthenticationFailure("Authentication cancelled")
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        // Too many attempts - offer PIN fallback
                        showPinFallback()
                    }
                    else -> {
                        // Other errors - show message and close app
                        Toast.makeText(this, "Authentication error: $errorMessage", Toast.LENGTH_LONG).show()
                        onAuthenticationFailure(errorMessage)
                    }
                }
            },
            onFailure = {
                // Authentication failed but user can try again
                Toast.makeText(this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Show dialog when biometrics are not enrolled
     */
    private fun showBiometricNotEnrolledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Biometric Not Set Up")
            .setMessage("No fingerprint or face recognition is set up on this device. Would you like to use a PIN instead?")
            .setPositiveButton("Use PIN") { _, _ ->
                showPinFallback()
            }
            .setNegativeButton("Close App") { _, _ ->
                onAuthenticationFailure("Biometric not enrolled")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Show PIN fallback authentication
     */
    private fun showPinFallback() {
        val intent = Intent(this, PinLockActivity::class.java).apply {
            putExtra(PinLockActivity.EXTRA_SETUP_MODE, !pinManager.isPinSet())
        }
        pinLauncher.launch(intent)
    }

    /**
     * Called when authentication succeeds
     */
    private fun onAuthenticationSuccess() {
        isAuthenticated = true
        isAuthenticationInProgress = false
        Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
        navigateToPendingDestination()
    }

    /**
     * Called when authentication fails or is cancelled
     */
    private fun onAuthenticationFailure(reason: String) {
        isAuthenticated = false
        isAuthenticationInProgress = false
        Toast.makeText(this, "Authentication failed: $reason", Toast.LENGTH_SHORT).show()
        // Close the entire app
        finishAffinity()
    }

    /**
     * Navigate to the pending destination after successful authentication
     */
    private fun navigateToPendingDestination() {
        pendingNavigation?.let {
            startActivity(it)
            finish()
        }
    }
}