package com.example.expensetracker.auth.Login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentLoginBinding
import com.example.expensetracker.utils.KeyboardUtils
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup keyboard dismiss when tapping outside EditText fields
        KeyboardUtils.setupHideKeyboardOnTouchRecursive(binding.root, requireActivity())

        setupUI()
        setupObservers()
    }

    /**
     * Setup UI click listeners
     */
    private fun setupUI() {
        // Navigate to Signup screen
        binding.signupText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        // Handle login button
        binding.loginButton.setOnClickListener {
            handleLogin()
        }
    }

    /**
     * Handle login button click
     */
    private fun handleLogin() {
        val email = binding.emailLoginEditText.text.toString().trim()
        val password = binding.passwordLoginEditText.text.toString()

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            NoInternetDialog.show(
                context = requireContext(),
                onRetry = { handleLogin() }
            )
            return
        }

        // Show progress bar while logging in
        binding.progressBar.visibility = View.VISIBLE
        viewModel.login(email, password)
    }

    /**
     * Setup LiveData observers
     */
    private fun setupObservers() {
        viewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE

            result.onSuccess { response ->
                // Show welcome toast
                Toast.makeText(
                    requireContext(),
                    "Welcome ${response.user.username}",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to MainActivity
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish() // Prevent going back to Login
            }

            result.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Login failed: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
