package com.example.expensetracker.auth.Login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.auth.TokenDataStore
import com.example.expensetracker.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Navigate to Signup screen
        binding.signupText.setOnClickListener {
            it.findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        // ✅ Observe login result ONCE here
        viewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE

            result.onSuccess { response ->
                // Show welcome toast
                Toast.makeText(
                    requireContext(),
                    "Welcome ${response.user.username}",
                    Toast.LENGTH_SHORT
                ).show()

                // Save token in DataStore
                val tokenDataStore = TokenDataStore(requireContext())
                lifecycleScope.launch {
                    tokenDataStore.saveToken(response.token)
                }

                // Navigate to MainActivity
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish() // Prevent going back to Login
            }

            result.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Login failed: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ✅ Handle login button
        binding.loginButton.setOnClickListener {
            val email = binding.emailLoginEditText.text.toString().trim()
            val password = binding.passwordLoginEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress bar while logging in
            binding.progressBar.visibility = View.VISIBLE
            viewModel.login(email, password)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
