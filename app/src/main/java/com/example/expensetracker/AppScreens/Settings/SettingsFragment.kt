package com.example.expensetracker.AppScreens.Settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.AppScreens.AddTransActivity
import com.example.expensetracker.SplashScreen
import com.example.expensetracker.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpObservers()

        // ✅ Set button click
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun setUpObservers() {
        // ✅ Observe logout state ONCE here
        viewModel.logoutState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show()
                // Navigate to Splash/Login screen
                startActivity(Intent(requireContext(), SplashScreen::class.java))
                requireActivity().finish() // finish current activity so user can’t go back
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Logout failed: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext()) // better than "context"
            .setTitle("Logout").setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout() // ✅ Trigger logout directly
            }.setNegativeButton("Cancel", null).show()
    }
}