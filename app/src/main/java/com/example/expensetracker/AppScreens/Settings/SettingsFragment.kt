package com.example.expensetracker.AppScreens.Settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.R
import com.example.expensetracker.auth.SplashScreen
import com.example.expensetracker.databinding.BottomSheetLanguageBinding
import com.example.expensetracker.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private var selectedLanguage = "English" // Default language

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpObservers()
        setUpClickListeners()
        updateLanguageDisplay()
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
            showLanguageBottomSheet()
        }
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

    private fun showLanguageBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = BottomSheetLanguageBinding.inflate(layoutInflater)

        // Create language list
        val languages = listOf(
            Language("en", getString(R.string.language_english), selectedLanguage == getString(R.string.language_english)),
            Language("es", getString(R.string.language_spanish), selectedLanguage == getString(R.string.language_spanish)),
            Language("fr", getString(R.string.language_french), selectedLanguage == getString(R.string.language_french)),
            Language("de", getString(R.string.language_german), selectedLanguage == getString(R.string.language_german)),
            Language("ar", getString(R.string.language_arabic), selectedLanguage == getString(R.string.language_arabic))
        )

        // Set up adapter
        val adapter = LanguageAdapter(languages) { selectedLang ->
            selectedLanguage = selectedLang.name
            updateLanguageDisplay()
            bottomSheetDialog.dismiss()

            // Show confirmation toast
            Toast.makeText(
                requireContext(),
                "Language changed to ${selectedLang.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        bottomSheetBinding.recyclerLanguages.adapter = adapter
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
    }

    private fun updateLanguageDisplay() {
        binding.valueLanguage.text = selectedLanguage
    }

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