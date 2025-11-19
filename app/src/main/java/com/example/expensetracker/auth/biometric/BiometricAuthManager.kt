package com.example.expensetracker.auth.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricAuthManager handles all biometric authentication logic.
 *
 * This manager:
 * - Checks if biometric authentication is available on the device
 * - Configures and shows the biometric prompt
 * - Handles authentication callbacks (success, error, failure)
 * - Provides fallback mechanisms when biometrics are unavailable
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null

    /**
     * Checks the biometric capability of the device
     *
     * @return BiometricStatus indicating the availability state
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(activity)

        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricStatus.AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricStatus.NO_HARDWARE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricStatus.HARDWARE_UNAVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricStatus.NONE_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricStatus.SECURITY_UPDATE_REQUIRED
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                BiometricStatus.UNSUPPORTED
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                BiometricStatus.UNKNOWN
            }
            else -> BiometricStatus.UNKNOWN
        }
    }

    /**
     * Initiates biometric authentication
     *
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when an error occurs (with error code and message)
     * @param onFailure Callback when authentication fails (wrong fingerprint, etc.)
     */
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        onFailure: () -> Unit
    ) {
        // Create biometric prompt with callbacks
        biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // User authenticated successfully
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Critical error occurred (user cancelled, too many attempts, etc.)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Authentication failed but user can try again (wrong fingerprint)
                    onFailure()
                }
            }
        )

        // Configure the prompt UI and allowed authentication methods
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Expense Tracker")
            .setSubtitle("Use your fingerprint, face, or device credentials")
            .setDescription("Authenticate to access your expense data")
            // Allow both biometric and device credentials (PIN, pattern, password)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        // Show the biometric prompt
        biometricPrompt?.authenticate(promptInfo!!)
    }

    /**
     * Cancels any ongoing authentication
     */
    fun cancelAuthentication() {
        biometricPrompt?.cancelAuthentication()
    }
}

/**
 * Enum representing different biometric availability states
 */
enum class BiometricStatus {
    AVAILABLE,              // Biometrics are available and enrolled
    NO_HARDWARE,           // Device doesn't have biometric hardware
    HARDWARE_UNAVAILABLE,  // Hardware is unavailable (maybe being used)
    NONE_ENROLLED,         // No biometrics enrolled (no fingerprint set up)
    SECURITY_UPDATE_REQUIRED, // Security update needed
    UNSUPPORTED,           // Feature not supported
    UNKNOWN                // Unknown status
}

