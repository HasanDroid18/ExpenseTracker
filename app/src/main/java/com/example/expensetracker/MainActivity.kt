package com.example.expensetracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.expensetracker.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Apply saved language before setting content view
        applySavedLanguage()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        handleWindowInsets()
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

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            binding.fragmentContainerView.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNav.visibility = if (imeVisible) View.GONE else View.VISIBLE

            if (!imeVisible) {
                binding.bottomNav.setPadding(0, 8, 0,  8)
            }
            insets
        }
    }
}