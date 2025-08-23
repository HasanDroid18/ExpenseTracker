package com.example.expensetracker.AppScreens.Home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.expensetracker.SplashScreen
import com.example.expensetracker.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Observe logout state ONCE here
        viewModel.logoutState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show()
                // Navigate to Splash/Login screen
                startActivity(Intent(requireContext(), SplashScreen::class.java))
                requireActivity().finish() // finish current activity so user can’t go back
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Logout failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Set button click
        binding.searchButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext()) // better than "context"
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout() // ✅ Trigger logout directly
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}