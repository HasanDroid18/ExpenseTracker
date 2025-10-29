package com.example.expensetracker.AppScreens.Home.AddTransaction

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.databinding.ActivityAddTransBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddTransActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransBinding
    private val viewModel: AddTransViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpObservers()
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnSave.setOnClickListener {
            saveCreateTrans()
        }

    }
    private fun saveCreateTrans() {
        val amountText = binding.etAmount.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val selectedChipId = binding.chipGroup.checkedChipId

        // Validate inputs first
        if (amountText.isEmpty() || title.isEmpty() || selectedChipId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Amount must be a number", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected category from ChipGroup
        val chip = findViewById<Chip>(selectedChipId)
        val category = chip.text.toString()

        binding.progressBar.visibility = View.VISIBLE
        viewModel.addTransaction(TransactionRequest(amount, category, title))
    }

    private fun setUpObservers(){
        viewModel.addTransactionResponse.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                Toast.makeText(this, e.message ?: "Failed to add transaction", Toast.LENGTH_SHORT).show()
            }
        }

    }

}
