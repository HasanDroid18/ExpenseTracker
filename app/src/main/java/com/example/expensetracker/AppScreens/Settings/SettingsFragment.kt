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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.R
import com.example.expensetracker.auth.SplashScreen
import com.example.expensetracker.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
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

        setupLanguageValue()
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

        // Ensure the layout direction stays Left-to-Right (LTR)
        requireActivity().window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        val intent = requireActivity().intent
        requireActivity().finish()
        startActivity(intent)
    }

//    private fun showLanguageBottomSheet() {
//        val bottomSheetDialog = BottomSheetDialog(requireContext())
//        val bottomSheetBinding = BottomSheetLanguageBinding.inflate(layoutInflater)
//
//        // Create language list
//        val languages = listOf(
//            Language("en", getString(R.string.language_english), selectedLanguage == getString(R.string.language_english)),
//            Language("es", getString(R.string.language_spanish), selectedLanguage == getString(R.string.language_spanish)),
//            Language("fr", getString(R.string.language_french), selectedLanguage == getString(R.string.language_french)),
//            Language("de", getString(R.string.language_german), selectedLanguage == getString(R.string.language_german)),
//            Language("ar", getString(R.string.language_arabic), selectedLanguage == getString(R.string.language_arabic))
//        )

        // Set up adapter
//        val adapter = LanguageAdapter(languages) { selectedLang ->
//            selectedLanguage = selectedLang.name
//            updateLanguageDisplay()
//            bottomSheetDialog.dismiss()
//
//            // Show confirmation toast
//            Toast.makeText(
//                requireContext(),
//                "Language changed to ${selectedLang.name}",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//
//        bottomSheetBinding.recyclerLanguages.adapter = adapter
//        bottomSheetDialog.setContentView(bottomSheetBinding.root)
//        bottomSheetDialog.show()
//    }
//
//    private fun updateLanguageDisplay() {
//        binding.valueLanguage.text = selectedLanguage
//    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext()) // better than "context"
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout() // ✅ Trigger logout directly
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}