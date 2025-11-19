package com.example.expensetracker.auth.biometric

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.databinding.ActivityPinLockBinding

/**
 * PinLockActivity provides a fallback PIN authentication mechanism
 * when biometric authentication is not available.
 *
 * This activity:
 * - Allows users to set up a PIN if none exists
 * - Verifies PIN for existing users
 * - Implements attempt throttling and temporary lockout for security
 * - Returns success/failure result to the calling activity
 *
 * Security Features:
 * - Progressive delay after 3 failed attempts
 * - 30-second lockout after 5 failed attempts
 * - 5-minute lockout after 10 failed attempts
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLockBinding
    private lateinit var pinManager: PinManager
    private lateinit var attemptManager: PinAttemptManager
    private var enteredPin = StringBuilder()
    private var isSettingUpPin = false
    private var firstPin = ""
    private var lockoutTimer: CountDownTimer? = null
    private var isInputEnabled = true

    companion object {
        const val EXTRA_SETUP_MODE = "setup_mode"
        const val RESULT_AUTHENTICATED = Activity.RESULT_OK
        const val RESULT_FAILED = Activity.RESULT_CANCELED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinManager = PinManager(this)
        attemptManager = PinAttemptManager(this)
        isSettingUpPin = intent.getBooleanExtra(EXTRA_SETUP_MODE, false) || !pinManager.isPinSet()

        // Check if locked out immediately
        if (!isSettingUpPin && attemptManager.isLockedOut()) {
            startLockoutTimer()
        }

        setupUI()
        setupNumberPad()
        updateAttemptWarning()
    }

    private fun setupUI() {
        if (isSettingUpPin) {
            binding.tvTitle.text = "Set Up PIN"
            binding.tvSubtitle.text = "Create a 4-digit PIN"
        } else {
            binding.tvTitle.text = "Enter PIN"
            binding.tvSubtitle.text = "Enter your 4-digit PIN to unlock"

            // Show failed attempts count if any
            val attempts = attemptManager.getFailedAttempts()
            if (attempts > 0) {
                binding.tvTitle.text = "Enter PIN ($attempts failed attempt${if (attempts > 1) "s" else ""})"
            }
        }

        updatePinDisplay()
    }

    private fun setupNumberPad() {
        // Number buttons
        binding.btn0.setOnClickListener { addDigit("0") }
        binding.btn1.setOnClickListener { addDigit("1") }
        binding.btn2.setOnClickListener { addDigit("2") }
        binding.btn3.setOnClickListener { addDigit("3") }
        binding.btn4.setOnClickListener { addDigit("4") }
        binding.btn5.setOnClickListener { addDigit("5") }
        binding.btn6.setOnClickListener { addDigit("6") }
        binding.btn7.setOnClickListener { addDigit("7") }
        binding.btn8.setOnClickListener { addDigit("8") }
        binding.btn9.setOnClickListener { addDigit("9") }

        // Delete button
        binding.btnDelete.setOnClickListener { deleteDigit() }

        // Cancel button (close app if cancelled)
        binding.btnCancel.setOnClickListener {
            setResult(RESULT_FAILED)
            finishAffinity() // Close the entire app
        }
    }

    private fun addDigit(digit: String) {
        // Check if input is enabled (not locked out or delayed)
        if (!isInputEnabled) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        if (enteredPin.length < 4) {
            enteredPin.append(digit)
            updatePinDisplay()

            // When 4 digits entered, verify or save PIN
            if (enteredPin.length == 4) {
                handlePinComplete()
            }
        }
    }

    private fun deleteDigit() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updatePinDisplay()
        }
    }

    private fun updatePinDisplay() {
        val pin = enteredPin.toString()
        binding.pinDot1.isActivated = pin.length >= 1
        binding.pinDot2.isActivated = pin.length >= 2
        binding.pinDot3.isActivated = pin.length >= 3
        binding.pinDot4.isActivated = pin.length >= 4
    }

    private fun handlePinComplete() {
        val pin = enteredPin.toString()

        if (isSettingUpPin) {
            // Setup mode - no attempt tracking needed
            if (firstPin.isEmpty()) {
                // First entry, ask to confirm
                firstPin = pin
                enteredPin.clear()
                binding.tvSubtitle.text = "Confirm your PIN"
                updatePinDisplay()
            } else {
                // Second entry, verify match
                if (firstPin == pin) {
                    if (pinManager.savePin(pin)) {
                        Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_AUTHENTICATED)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to save PIN", Toast.LENGTH_SHORT).show()
                        resetPinSetup()
                    }
                } else {
                    Toast.makeText(this, "PINs don't match. Try again.", Toast.LENGTH_SHORT).show()
                    resetPinSetup()
                }
            }
        } else {
            // Verify existing PIN - WITH ATTEMPT TRACKING
            if (pinManager.verifyPin(pin)) {
                // SUCCESS - Reset attempts and proceed
                attemptManager.resetAttempts()
                Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
                setResult(RESULT_AUTHENTICATED)
                finish()
            } else {
                // FAILURE - Record attempt and apply security measures
                attemptManager.recordFailedAttempt()

                val attempts = attemptManager.getFailedAttempts()
                binding.tvTitle.text = "Enter PIN ($attempts failed attempt${if (attempts > 1) "s" else ""})"

                // Check if now locked out
                if (attemptManager.isLockedOut()) {
                    startLockoutTimer()
                } else {
                    // Apply throttling delay if threshold reached
                    val delay = attemptManager.getAttemptDelay()
                    if (delay > 0) {
                        applyThrottlingDelay(delay)
                    } else {
                        Toast.makeText(this, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                enteredPin.clear()
                updatePinDisplay()
                updateAttemptWarning()
            }
        }
    }

    private fun resetPinSetup() {
        firstPin = ""
        enteredPin.clear()
        binding.tvSubtitle.text = "Create a 4-digit PIN"
        updatePinDisplay()
    }

    /**
     * Start lockout countdown timer and disable input
     */
    private fun startLockoutTimer() {
        val remainingMs = attemptManager.getRemainingLockoutTime()

        if (remainingMs <= 0) return

        // Disable input
        isInputEnabled = false
        disableNumberPad()

        // Show lockout message
        binding.tvSubtitle.text = attemptManager.getLockoutMessage()

        // Start countdown
        lockoutTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.tvSubtitle.text = when {
                    secondsRemaining < 60 -> "Locked out. Try again in $secondsRemaining seconds"
                    else -> {
                        val minutes = secondsRemaining / 60
                        "Locked out. Try again in $minutes minute${if (minutes > 1) "s" else ""}"
                    }
                }
            }

            override fun onFinish() {
                // Re-enable input
                isInputEnabled = true
                enableNumberPad()
                binding.tvSubtitle.text = "Enter your 4-digit PIN to unlock"
                Toast.makeText(this@PinLockActivity, "You may try again now", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    /**
     * Apply progressive delay before next attempt
     */
    private fun applyThrottlingDelay(delayMs: Long) {
        isInputEnabled = false
        disableNumberPad()

        val delaySeconds = (delayMs / 1000).toInt()
        binding.tvSubtitle.text = "Too many attempts. Wait $delaySeconds seconds..."

        // Use a simple countdown
        object : CountDownTimer(delayMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.tvSubtitle.text = "Wait $secondsRemaining second${if (secondsRemaining > 1) "s" else ""}..."
            }

            override fun onFinish() {
                isInputEnabled = true
                enableNumberPad()
                binding.tvSubtitle.text = "Enter your 4-digit PIN to unlock"
            }
        }.start()
    }

    /**
     * Update warning message based on attempt count
     */
    private fun updateAttemptWarning() {
        if (isSettingUpPin) return

        val warning = attemptManager.getWarningMessage()
        if (warning.isNotEmpty()) {
            Toast.makeText(this, warning, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Disable number pad visually and functionally
     */
    private fun disableNumberPad() {
        binding.btn0.isEnabled = false
        binding.btn1.isEnabled = false
        binding.btn2.isEnabled = false
        binding.btn3.isEnabled = false
        binding.btn4.isEnabled = false
        binding.btn5.isEnabled = false
        binding.btn6.isEnabled = false
        binding.btn7.isEnabled = false
        binding.btn8.isEnabled = false
        binding.btn9.isEnabled = false
        binding.btnDelete.isEnabled = false

        // Visual feedback
        binding.numberPad.alpha = 0.4f
    }

    /**
     * Enable number pad
     */
    private fun enableNumberPad() {
        binding.btn0.isEnabled = true
        binding.btn1.isEnabled = true
        binding.btn2.isEnabled = true
        binding.btn3.isEnabled = true
        binding.btn4.isEnabled = true
        binding.btn5.isEnabled = true
        binding.btn6.isEnabled = true
        binding.btn7.isEnabled = true
        binding.btn8.isEnabled = true
        binding.btn9.isEnabled = true
        binding.btnDelete.isEnabled = true

        // Visual feedback
        binding.numberPad.alpha = 1.0f
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Don't allow back press - must authenticate or cancel
        // User must use the Cancel button to close the app
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up timer
        lockoutTimer?.cancel()
    }
}



