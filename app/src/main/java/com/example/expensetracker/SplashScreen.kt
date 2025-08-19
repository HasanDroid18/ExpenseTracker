package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.auth.AuthActivity
import com.example.expensetracker.auth.Login.LoginFragment
import com.example.expensetracker.auth.TokenDataStore
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

        val tokenDataStore = TokenDataStore(this)

        lifecycleScope.launch {
            // wait 3 seconds for splash
            kotlinx.coroutines.delay(3000)

            tokenDataStore.tokenFlow.collect { token ->
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
