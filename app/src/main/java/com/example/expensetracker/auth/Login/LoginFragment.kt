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

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentLoginBinding.inflate(inflater, container, false)
        binding.signupText.setOnClickListener {
            it.findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
        viewModel.loginResponse.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { response ->
                startActivity(Intent(requireContext() , MainActivity::class.java))
                Toast.makeText(requireContext(), "Welcome ${response.user.username}", Toast.LENGTH_SHORT).show()
                // Save token to SharedPreferences / DataStore if needed
                // Suppose login is successful and you have a token
                val tokenDataStore = TokenDataStore(requireContext())
                lifecycleScope.launch {
                    tokenDataStore.saveToken(response.token)
                }

            }
            result.onFailure {
                Toast.makeText(requireContext(), "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.loginButton.setOnClickListener {
            val email = binding.emailLoginEditText.text.toString().trim()
            val password = binding.passwordLoginEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            viewModel.login(email, password)
        }
        return binding.root
    }



}