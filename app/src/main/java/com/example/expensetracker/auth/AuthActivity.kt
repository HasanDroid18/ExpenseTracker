package com.example.expensetracker.auth

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.expensetracker.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved language before setting content view
        applySavedLanguage()

        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        navController=findNavController(R.id.fragmentContainerViewAuth)
        return navController.navigateUp() || super.onSupportNavigateUp()
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
}