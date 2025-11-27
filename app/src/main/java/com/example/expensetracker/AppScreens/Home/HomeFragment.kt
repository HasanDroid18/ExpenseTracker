package com.example.expensetracker.AppScreens.Home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.R
import com.example.expensetracker.AppScreens.Home.AddTransaction.AddTransActivity
import com.example.expensetracker.databinding.FragmentHomeBinding
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
import dagger.hilt.android.AndroidEntryPoint
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

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

        if (isFirstLoad) {
            viewModel.loadDataIfNeeded()
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupUI() {
        binding.addTransButton.setOnClickListener {
            // Navigate to Add Transaction screen
            startActivity(Intent(requireContext(), AddTransActivity::class.java))
        }

        setupChart()
        // Swipe-to-refresh action
        binding.swipeRefresh.setOnRefreshListener {
            userRefreshing = true
            viewModel.refreshData() // Force refresh
        }
    }

    private fun setupObservers() {
        // Observe username
        viewModel.username.observe(viewLifecycleOwner) { username ->
            binding.UserName.text = username
        }

        // Observe summary data
        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            summary?.let {
                replaceNumericPart(binding.totalBalance, it.balance)
                replaceNumericPart(binding.income, it.income)
                replaceNumericPart(binding.expense, it.expenses)
                renderSummary(it)
            } ?: run {
                binding.barChart.clear()
                binding.barChart.invalidate()
            }
        }


        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Check if it's a network error
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    NoInternetDialog.show(
                        context = requireContext(),
                        onRetry = { viewModel.refreshData() }
                    )
                } else {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun setupChart() {
        val chart = binding.barChart
        chart.description.isEnabled = false
        chart.setNoDataText(getString(R.string.chart_no_data))
        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.legend.isEnabled = true
        chart.setFitBars(true)
        chart.extraTopOffset = 8f
        chart.extraBottomOffset = 8f

        chart.legend.apply {
            textSize = 12f
            formSize = 10f
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            axisMinimum = -0.5f
            axisMaximum = 2.5f
            textSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = when (value.toInt()) {
                    0 -> getString(R.string.chart_label_income)
                    1 -> getString(R.string.chart_label_expenses)
                    2 -> getString(R.string.chart_label_balance)
                    else -> ""
                }
            }
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
            granularity = 1f
            textSize = 12f
            axisMinimum = 0f // will be updated on data render
        }
    }

    private fun renderSummary(summary: SummaryResponse) {
        val chart = binding.barChart

        // Parse currency strings to Float values
        val incomeValue = parseCurrencyToFloat(summary.income)
        val expensesValue = parseCurrencyToFloat(summary.expenses)
        val balanceValue = parseCurrencyToFloat(summary.balance)

        val values = listOf(incomeValue, expensesValue, balanceValue)

        val entries = values.mapIndexed { idx, v -> BarEntry(idx.toFloat(), v) }

        val dataSet = BarDataSet(entries, getString(R.string.chart_label_totals)).apply {
            // Distinct colors for each bar
            colors = listOf(
                "#2E7D32".toColorInt(), // Income - green
                "#C62828".toColorInt(), // Expenses - red
                "#1565C0".toColorInt()  // Balance - blue
            )
            valueTextColor = 0xFF444444.toInt()
            valueTextSize = 12f
            setDrawValues(true)
            highLightAlpha = 60
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.6f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "0" else String.format(Locale.getDefault(), "%.2f", value)
                }
            })
        }

        // Compute a visible Y range even when values may be negative/zero
        val maxY = values.maxOrNull() ?: 0f
        val minY = values.minOrNull() ?: 0f
        chart.axisLeft.axisMaximum = if (maxY <= 0f) 1f else maxY * 1.1f
        chart.axisLeft.axisMinimum = if (minY < 0f) minY * 1.1f else 0f

        chart.data = data
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.animateY(600)
        chart.invalidate()
    }

    /**
     * Parses a currency string (e.g., "$123.45" or "123.45") to a Float value
     */
    private fun parseCurrencyToFloat(value: String): Float {
        return try {
            // Remove $ sign, commas, and any other non-numeric characters except . and -
            val cleanValue = value.replace(Regex("[^0-9.,-]"), "")
            cleanValue.toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

}