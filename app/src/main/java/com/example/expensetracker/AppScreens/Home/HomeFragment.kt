package com.example.expensetracker.AppScreens.Home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.AppScreens.Home.AddTransaction.AddTransActivity
import com.example.expensetracker.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import android.widget.TextView

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private val viewModel: HomeViewModel by viewModels()

    private var userRefreshing = false
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        // Only load data on first creation, not on subsequent view recreations
        if (isFirstLoad) {
            viewModel.loadDataIfNeeded()
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()

        // Only load data if needed (cache expired or data stale)
        // This prevents unnecessary API calls when returning from other screens
        viewModel.loadDataIfNeeded()
    }

    private fun setupUI() {
        binding.addTransButton.setOnClickListener {
            // Navigate to Add Transaction screen
            startActivity(Intent(requireContext(), AddTransActivity::class.java))
        }

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        // Swipe-to-refresh action
        binding.swipeRefresh.setOnRefreshListener {
            userRefreshing = true
            viewModel.refreshData() // Force refresh
        }
    }

    private fun setupObservers() {
        // Observe LiveData
        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            binding.rvTransactions.adapter = TransactionAdapter(list) { txn ->
                viewModel.deleteTransaction(txn.id)
            }
        }

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            summary?.let {
                replaceNumericPart(binding.totalBalance, it.balance)
                replaceNumericPart(binding.income, it.income)
                replaceNumericPart(binding.expense, it.expenses)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Show overlay only for non-user initiated loads
            binding.progressBar2.visibility = if (loading && !userRefreshing) View.VISIBLE else View.GONE
            // Show swipe spinner only for user refresh
            binding.swipeRefresh.isRefreshing = loading && userRefreshing
            if (!loading) userRefreshing = false
        }

    }

    private fun replaceNumericPart(tv: TextView, newValue: String?) {

        val amountRaw = newValue?.ifBlank { "$0.00" } ?: "$0.00"
        
        // If the value doesn't start with $, add it
        val formattedValue = if (!amountRaw.startsWith("$")) {
            val cleanNumber = amountRaw.replace(Regex("[^0-9.,-]"), "").ifBlank { "0.00" }
            "$$cleanNumber"
        } else {
            amountRaw
        }
        
        // Simply set the text directly since we're ensuring $ prefix
        tv.text = formattedValue
    }

}