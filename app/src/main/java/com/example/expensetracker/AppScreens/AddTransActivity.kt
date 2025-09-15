package com.example.expensetracker.AppScreens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.example.expensetracker.databinding.ActivityAddTransBinding

class AddTransActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topAppBar.setNavigationOnClickListener  {
            onBackPressedDispatcher.onBackPressed()
        }
    }


}