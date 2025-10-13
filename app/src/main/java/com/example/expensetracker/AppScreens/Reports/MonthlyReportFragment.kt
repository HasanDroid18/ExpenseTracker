package com.example.expensetracker.AppScreens.Reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.databinding.FragmentMonthlyReportBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.toColorInt
import java.util.Calendar
import android.widget.Toast
import java.util.Locale

@AndroidEntryPoint
class MonthlyReportFragment : Fragment() {

    private var _binding: FragmentMonthlyReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MonthlyReportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupObservers()

        // Default to current month/year
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        viewModel.loadMonthlyReport(year, month)
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

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        // Only observe the monthly summary to render a compact 3-bar chart
        viewModel.monthlySummary.observe(viewLifecycleOwner) { summary ->
            if (summary == null) {
                binding.barChart.clear()
                binding.barChart.invalidate()
                return@observe
            }
            renderMonthlySummary(summary)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
