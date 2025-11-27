package com.example.expensetracker.AppScreens.Settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.R
import com.example.expensetracker.auth.SplashScreen
import com.example.expensetracker.auth.biometric.BiometricAuthManager
import com.example.expensetracker.auth.biometric.BiometricPreferenceManager
import com.example.expensetracker.auth.biometric.PinLockActivity
import com.example.expensetracker.auth.biometric.PinManager
import com.example.expensetracker.databinding.FragmentSettingsBinding
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var biometricPreferenceManager: BiometricPreferenceManager
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var pinManager: PinManager
    private var pendingSecurityToggleState: Boolean? = null

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
    }

    // Activity result launcher for PIN authentication when toggling security
    private val pinLauncherForToggle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == PinLockActivity.RESULT_AUTHENTICATED) {
            // Authentication successful - apply the pending toggle
            applySecurityToggle()
        } else {
            // Authentication failed - revert switch to previous state
            binding.switchBiometric.isChecked = !binding.switchBiometric.isChecked
            pendingSecurityToggleState = null
            Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        biometricPreferenceManager = BiometricPreferenceManager(requireContext())
        biometricAuthManager = BiometricAuthManager(requireActivity())
        pinManager = PinManager(requireContext())

        setupLanguageValue()
        setupBiometricSwitch()
        setUpObservers()
        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // About Us card
        binding.rowAboutUs.setOnClickListener {
            showAboutUsDialog()
        }

        // Language card
        binding.rowLanguage.setOnClickListener {
            showBottomDialogLanguage()
        }

        // Biometric row click - toggle the switch
        binding.rowBiometric.setOnClickListener {
            binding.switchBiometric.isChecked = !binding.switchBiometric.isChecked
        }
    }

    /**
     * Setup biometric security switch with current state and change listener
     *
     * ⚠️ SECURITY: Requires authentication before allowing ANY toggle change
     */
    private fun setupBiometricSwitch() {
        // Set initial state from saved preference
        binding.switchBiometric.isChecked = biometricPreferenceManager.isBiometricEnabled()

        // Handle switch changes - REQUIRE AUTHENTICATION FOR ANY CHANGE
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            // Store the pending state
            pendingSecurityToggleState = isChecked

            // Check if security is currently enabled
            val currentlyEnabled = biometricPreferenceManager.isBiometricEnabled()

            if (currentlyEnabled != isChecked) {
                // User is trying to CHANGE security state - MUST AUTHENTICATE
                // This applies to BOTH enabling AND disabling
                requestAuthenticationForToggle()
            } else {
                // No change, reset pending state
                pendingSecurityToggleState = null
            }
        }
    }

    /**
     * Request authentication before allowing security toggle
     * Uses biometric if available, falls back to PIN
     */
    private fun requestAuthenticationForToggle() {
        val biometricStatus = biometricAuthManager.checkBiometricAvailability()

        when (biometricStatus) {
            com.example.expensetracker.auth.biometric.BiometricStatus.AVAILABLE -> {
                // Show biometric prompt
                showBiometricPromptForToggle()
            }
            else -> {
                // No biometric available, check if PIN is set
                if (pinManager.isPinSet()) {
                    // Launch PIN screen
                    val intent = Intent(requireContext(), PinLockActivity::class.java)
                    pinLauncherForToggle.launch(intent)
                } else {
                    // No security set up, revert toggle and warn user
                    binding.switchBiometric.isChecked = !binding.switchBiometric.isChecked
                    pendingSecurityToggleState = null
                    Toast.makeText(
                        requireContext(),
                        "Cannot change security settings - no authentication method configured",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Show biometric prompt to authenticate security toggle
     */
    private fun showBiometricPromptForToggle() {
        biometricAuthManager.authenticate(
            onSuccess = {
                // Authentication successful - apply the toggle
                applySecurityToggle()
            },
            onError = { errorCode, errString ->
                // Authentication error - revert switch
                binding.switchBiometric.isChecked = !binding.switchBiometric.isChecked
                pendingSecurityToggleState = null

                // If biometric failed, try PIN fallback
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                    errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    if (pinManager.isPinSet()) {
                        Toast.makeText(requireContext(), "Too many attempts. Use PIN.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), PinLockActivity::class.java)
                        pinLauncherForToggle.launch(intent)
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed: $errString", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Authentication cancelled", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                // Failed attempt but can retry - keep switch in pending state
                Toast.makeText(requireContext(), "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Apply the pending security toggle after successful authentication
     */
    private fun applySecurityToggle() {
        pendingSecurityToggleState?.let { newState ->
            biometricPreferenceManager.setBiometricEnabled(newState)

            val message = if (newState) {
                "Biometric security enabled"
            } else {
                "Biometric security disabled"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            pendingSecurityToggleState = null
        }
    }

    private fun setupLanguageValue() {
        val currentLanguage = getCurrentLanguage()
        binding.valueLanguage.text = currentLanguage
        binding.iconLanguageSave.visibility = View.VISIBLE
    }

    private fun getCurrentLanguage(): String {
        // First check SharedPreferences
        val savedLang = sharedPreferences.getString(KEY_LANGUAGE, null)
        if (savedLang != null) {
            return savedLang
        }

        // Otherwise check system locale
        val locale = requireContext().resources.configuration.locales[0]
        return when (locale.language) {
            "ar" -> "Arabic"
            "en" -> "English"
            else -> "English"
        }
    }

    private fun saveLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    private fun setUpObservers() {
        // ✅ Observe logout state ONCE here
        viewModel.logoutState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show()
                // Navigate to Splash/Login screen
                startActivity(Intent(requireContext(), SplashScreen::class.java))
                requireActivity().finish() // finish current activity so user can't go back
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Logout failed: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // ✅ Observe username and email from DataStore via ViewModel and display them
        viewModel.username.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name ?: getString(R.string.default_username)
        }
        viewModel.email.observe(viewLifecycleOwner) { mail ->
            binding.tvUserEmail.text = mail ?: getString(R.string.default_email)
        }
    }

    private fun showAboutUsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.about_us_title))
            .setMessage(getString(R.string.about_us_message))
            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.logo) // Using app logo if available
            .show()
    }



    private fun showBottomDialogLanguage() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheet_language)

        val arabicLayout: LinearLayout = dialog.findViewById(R.id.arabicLayout)
        val englishLayout: LinearLayout = dialog.findViewById(R.id.englishLayout)
        val iconArabicSave: View = dialog.findViewById(R.id.iconArabicSave)
        val iconEnglishSave: View = dialog.findViewById(R.id.iconEnglishSave)

        // Show save icon for the current language
        val currentLanguage = getCurrentLanguage()
        if (currentLanguage == "Arabic") {
            iconArabicSave.visibility = View.VISIBLE
            iconEnglishSave.visibility = View.GONE
        } else {
            iconEnglishSave.visibility = View.VISIBLE
            iconArabicSave.visibility = View.GONE
        }

        arabicLayout.setOnClickListener {
            dialog.dismiss()
            saveLanguage("Arabic")
            binding.valueLanguage.text = "Arabic"
            binding.iconLanguageSave.visibility = View.VISIBLE
            setLocale("ar")
            Toast.makeText(requireContext(), "Language changed to Arabic", Toast.LENGTH_SHORT).show()
        }

        englishLayout.setOnClickListener {
            dialog.dismiss()
            saveLanguage("English")
            binding.valueLanguage.text = "English"
            binding.iconLanguageSave.visibility = View.VISIBLE
            setLocale("en")
            Toast.makeText(requireContext(), "Language changed to English", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes?.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        // Apply the configuration
        resources.updateConfiguration(config, resources.displayMetrics)

        // Set proper layout direction based on language
        requireActivity().window.decorView.layoutDirection = if (languageCode == "ar") {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }

        val intent = requireActivity().intent
        requireActivity().finish()
        startActivity(intent)
    }


    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Check network connectivity before logout
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    NoInternetDialog.show(
                        context = requireContext(),
                        onRetry = { showLogoutConfirmationDialog() }
                    )
                } else {
                    viewModel.logout()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}