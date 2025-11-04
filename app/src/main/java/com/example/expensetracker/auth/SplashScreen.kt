package com.example.expensetracker.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userDataStore = UserDataStore(this)

        lifecycleScope.launch {
            // wait 3 seconds for splash
            delay(3000)

            userDataStore.tokenFlow.collect { token ->
                if (!token.isNullOrEmpty()) {
                    // User already logged in, go to Main screen
                    startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                } else {
                    // Not logged in, go to Login screen
                    startActivity(Intent(this@SplashScreen, AuthActivity::class.java))
                }
                finish() // close splash
                return@collect // stop collecting after first navigation
            }
        }
    }
}