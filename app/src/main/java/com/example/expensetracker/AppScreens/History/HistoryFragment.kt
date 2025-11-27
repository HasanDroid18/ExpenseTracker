package com.example.expensetracker.AppScreens.History

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.databinding.FragmentHistoryBinding
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh transactions when fragment is resumed (e.g., after adding/deleting a transaction)
        viewModel.loadDataIfNeeded(forceRefresh = true)
    }

    /**
     * Setup RecyclerView and UI components
     */
    private fun setupUI() {
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // Setup swipe-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDataIfNeeded(forceRefresh = true)
        }
    }

    /**
     * Setup LiveData observers
     */
    private fun setupObservers() {
        // Observe transactions list
        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            binding.rvTransactions.adapter = TransactionAdapter(list) { transaction ->
                viewModel.deleteTransaction(transaction.id)
            }
        }

        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Check if it's a network error
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    NoInternetDialog.show(
                        context = requireContext(),
                        onRetry = { viewModel.loadDataIfNeeded(forceRefresh = true) }
                    )
                } else {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.rvTransactions.alpha = if (isLoading) 0.5f else 1f
            // Stop swipe refresh animation when loading is done
            if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

}