package com.example.expensetracker.auth.SignUp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginText.setOnClickListener {
            it.findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        setUpObservers()
        binding.signupButton.setOnClickListener {
            setUpSignupBtn()
        }
    }
    private fun setUpSignupBtn(){
        val email = binding.emailSignupEditText.text.toString().trim()
        val username = binding.userNameSignupEditText.text.toString().trim()
        val password = binding.passwordSignupEditText.text.toString()

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress bar while signing up
        binding.progressBar.visibility = View.VISIBLE
        viewModel.signup(email, username, password)
    }

    private fun setUpObservers() {
        viewModel.signupResponse.observe(viewLifecycleOwner){result ->
            binding.progressBar.visibility= View.GONE
            result.onSuccess { response ->
                Toast.makeText(requireContext(), "Account created successfully", Toast.LENGTH_SHORT).show()
                // Navigate to login screen after successful signup
                view?.findNavController()?.navigate(R.id.action_signupFragment_to_loginFragment)
            }
            result.onFailure { error ->
                // Handle error
                error.printStackTrace()
            }
        }
    }


}