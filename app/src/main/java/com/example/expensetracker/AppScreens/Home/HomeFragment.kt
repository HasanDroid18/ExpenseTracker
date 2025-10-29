package com.example.expensetracker.AppScreens.Home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.AppScreens.Home.AddTransaction.AddTransActivity
import com.example.expensetracker.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.example.expensetracker.AppScreens.Home.MonthlySummaryResponse
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

        // Only load data on first creation, not on subsequent view recreations
        if (isFirstLoad) {
            viewModel.loadDataIfNeeded()
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()

        // Force refresh data when the fragment is resumed to ensure it's always up-to-date.
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
        // Observe summary data
        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            summary?.let {
                replaceNumericPart(binding.totalBalance, it.balance)
                replaceNumericPart(binding.income, it.income)
                replaceNumericPart(binding.expense, it.expenses)
            }
        }

        // Observe monthly summary data
        viewModel.monthlySummary.observe(viewLifecycleOwner) { summary ->
            if (summary == null) {
                binding.barChart.clear()
                binding.barChart.invalidate()
            } else {
                renderMonthlySummary(summary)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
        chart.setNoDataText("No data for this month")
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
                    0 -> "Income"
                    1 -> "Expenses"
                    2 -> "Balance"
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

    private fun renderMonthlySummary(summary: MonthlySummaryResponse) {
        val chart = binding.barChart

        val values = listOf(
            (summary.income ?: 0.0).toFloat(),
            (summary.expenses ?: 0.0).toFloat(),
            (summary.balance ?: 0.0).toFloat()
        )

        val entries = values.mapIndexed { idx, v -> BarEntry(idx.toFloat(), v) }

        val dataSet = BarDataSet(entries, "Monthly totals").apply {
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

}