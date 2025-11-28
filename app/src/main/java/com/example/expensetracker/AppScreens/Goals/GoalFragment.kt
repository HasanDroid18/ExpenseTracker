package com.example.expensetracker.AppScreens.Goals

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentGoalBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

/**
 * Fragment for displaying and managing monthly expense goals
 * Shows goal amount, progress, current expenses, and allows editing
 */
@AndroidEntryPoint
class GoalFragment : Fragment() {

    private lateinit var binding: FragmentGoalBinding
    private val viewModel: GoalViewModel by viewModels()

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Notifications disabled. You won't receive milestone alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize notification channel
        GoalNotificationBuilder.createNotificationChannel(requireContext())

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Schedule WorkManager for periodic checking
        scheduleGoalCheckWorker()

        // Setup observers
        setupObservers()

        // Setup click listeners
        setupClickListeners()

        // Note: Initial data load happens in onResume()
    }

    /**
     * Request notification permission on Android 13+ (API 33+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation dialog
                    AlertDialog.Builder(requireContext())
                        .setTitle("Notification Permission Needed")
                        .setMessage("This app needs notification permission to alert you when you reach 50%, 80%, and 100% of your monthly expense goal.")
                        .setPositiveButton("Grant") { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> {
                    // Request permission directly
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * Schedule WorkManager to check goal progress periodically
     * Runs every 6 hours to check expenses and send notifications
     */
    private fun scheduleGoalCheckWorker() {
        val workRequest = PeriodicWorkRequestBuilder<GoalCheckWorker>(
            6, TimeUnit.HOURS // Check every 6 hours
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            GoalCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            workRequest
        )
    }

    /**
     * Setup LiveData observers
     */
    private fun setupObservers() {
        // Observe goal amount
        viewModel.goalAmount.observe(viewLifecycleOwner) { goalAmount ->
            if (goalAmount != null && goalAmount > 0) {
                // Goal is set - show goal view
                binding.layoutNoGoal.visibility = View.GONE
                binding.layoutGoalSet.visibility = View.VISIBLE
                binding.tvGoalAmount.text = "$${String.format("%.2f", goalAmount)}"
            } else {
                // No goal set - show empty state
                binding.layoutNoGoal.visibility = View.VISIBLE
                binding.layoutGoalSet.visibility = View.GONE
            }
        }

        // Observe current expenses
        viewModel.currentExpenses.observe(viewLifecycleOwner) { expenses ->
            android.util.Log.d("GoalFragment", "💰 Expenses updated: $$expenses")
            binding.tvCurrentExpenses.text = "$${String.format("%.2f", expenses)}"
        }

        // Observe progress percentage
        viewModel.progressPercent.observe(viewLifecycleOwner) { progress ->
            android.util.Log.d("GoalFragment", "📊 Progress updated: $progress%")
            binding.progressBar.progress = progress.coerceIn(0, 100)
            binding.tvProgressPercent.text = "$progress%"

            // Update progress bar color based on percentage
            val colorResId = viewModel.getProgressColor()
            binding.progressBar.progressTintList = ContextCompat.getColorStateList(
                requireContext(),
                colorResId
            )

            // Show warning if exceeded
            if (progress >= 100) {
                binding.tvWarning.visibility = View.VISIBLE
                binding.tvWarning.text = "⚠️ You've exceeded your monthly goal!"
            } else if (progress >= 80) {
                binding.tvWarning.visibility = View.VISIBLE
                binding.tvWarning.text = "⚠️ You're close to your limit!"
            } else {
                binding.tvWarning.visibility = View.GONE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSetGoal.isEnabled = !isLoading
            binding.btnEditGoal.isEnabled = !isLoading
        }

        // Observe save goal result
        viewModel.saveGoalState.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSaveGoalState() // Clear after consumption
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
                    viewModel.clearSaveGoalState() // Clear after consumption
                }
            }
        }

        // Observe delete goal result
        viewModel.deleteGoalState.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearDeleteGoalState() // Clear after consumption
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
                    viewModel.clearDeleteGoalState() // Clear after consumption
                }
            }
        }
    }

    /**
     * Setup click listeners for buttons
     */
    private fun setupClickListeners() {
        // Set Goal button (shown when no goal is set)
        binding.btnSetGoal.setOnClickListener {
            showSetGoalDialog()
        }

        // Edit Goal button (shown when goal is set)
        binding.btnEditGoal.setOnClickListener {
            showSetGoalDialog(viewModel.goalAmount.value)
        }

        // Delete Goal button
        binding.btnDeleteGoal.setOnClickListener {
            showDeleteGoalConfirmation()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshExpensesAndProgress()
            Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show()
        }

        // Test Notification button (long press refresh button for debugging)
        binding.btnRefresh.setOnLongClickListener {
            android.util.Log.d("GoalFragment", "🔔 Sending test notification...")
            GoalNotificationBuilder.sendTestNotification(requireContext())
            Toast.makeText(requireContext(), "Test notification sent! Check notification panel.", Toast.LENGTH_LONG).show()
            true
        }
    }

    /**
     * Show dialog to set or edit goal amount
     * @param currentGoal Current goal amount (null if setting new goal)
     */
    private fun showSetGoalDialog(currentGoal: Double? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_set_goal, null)

        val etGoalAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etGoalAmount
        )

        // Pre-fill with current goal if editing
        if (currentGoal != null) {
            etGoalAmount.setText(currentGoal.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (currentGoal == null) "Set Monthly Goal" else "Edit Monthly Goal")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val amountText = etGoalAmount.text.toString()

                if (amountText.isBlank()) {
                    Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val amount = amountText.toDouble()
                    viewModel.saveGoal(amount)
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show confirmation dialog before deleting goal
     */
    private fun showDeleteGoalConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete your monthly expense goal?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGoal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (menuVisible && isResumed) {
            android.util.Log.d("GoalFragment", "👁️ Fragment became visible via bottom nav - refreshing")
            // This is called when switching tabs in bottom navigation
            viewModel.refreshExpensesAndProgress()
        }
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("GoalFragment", "🟢 onStart() called - fragment becoming visible")
        // Refresh when fragment starts (more reliable than onResume for fragments)
        viewModel.refreshExpensesAndProgress()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("GoalFragment", "📱 onResume() called - fragment fully resumed")
        // Also refresh on resume as backup
        viewModel.refreshExpensesAndProgress()
    }
}

