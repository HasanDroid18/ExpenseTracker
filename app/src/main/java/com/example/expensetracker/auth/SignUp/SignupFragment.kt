package com.example.expensetracker.auth.SignUp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentSignupBinding
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.NoInternetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    /**
     * Setup UI click listeners
     */
    private fun setupUI() {
        // Navigate back to login screen
        binding.loginText.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        // Handle signup button
        binding.signupButton.setOnClickListener {
            handleSignup()
        }
    }

    /**
     * Handle signup button click
     */
    private fun handleSignup() {
        val email = binding.emailSignupEditText.text.toString().trim()
        val username = binding.userNameSignupEditText.text.toString().trim()
        val password = binding.passwordSignupEditText.text.toString()

        // Validate input
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            NoInternetDialog.show(
                context = requireContext(),
                onRetry = { handleSignup() }
            )
            return
        }

        // Show progress bar while signing up
        binding.progressBar.visibility = View.VISIBLE
        viewModel.signup(email, username, password)
    }

    /**
     * Setup LiveData observers
     */
    private fun setupObservers() {
        viewModel.signupResponse.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE

            result.onSuccess { response ->
                Toast.makeText(
                    requireContext(),
                    "Account created successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to login screen after successful signup
                findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
            }

            result.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Signup failed: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}