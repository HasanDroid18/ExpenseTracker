package com.example.expensetracker.AppScreens.Home.AddTransaction

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.R
import com.example.expensetracker.databinding.ActivityAddTransBinding
import com.example.expensetracker.utils.KeyboardUtils
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
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

        // Setup keyboard dismiss when tapping outside EditText fields
        KeyboardUtils.setupHideKeyboardOnTouchRecursive(binding.root, this)

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
            Toast.makeText(this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }

        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(this)) {
            NoInternetDialog.show(
                context = this,
                onRetry = { saveCreateTrans() }
            )
            return
        }

        // Get selected category based on chip ID (not text, to avoid translation issues)
        val category = when (selectedChipId) {
            R.id.chip_income -> "income"
            R.id.chip_expense -> "expense"
            else -> "expense" // default fallback
        }

        binding.progressBar.visibility = View.VISIBLE
        viewModel.addTransaction(TransactionRequest(amount, category, title))
    }

    private fun setUpObservers(){
        viewModel.addTransactionResponse.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this, getString(R.string.success_transaction_added), Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                Toast.makeText(this, e.message ?: getString(R.string.error_transaction_failed), Toast.LENGTH_SHORT).show()
            }
        }

    }

}
