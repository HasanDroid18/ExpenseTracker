package com.example.expensetracker.AppScreens.Converter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentConverterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConverterFragment : Fragment() {

    private var _binding: FragmentConverterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConverterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.convertBtn.setOnClickListener {
            val amount = binding.amountInput.text.toString().toDoubleOrNull()
            val mode = binding.currencySpinner.selectedItem?.toString() ?: ""
            if (amount != null) {
                viewModel.convert(amount, mode)
            } else {
                binding.amountInput.error = getString(R.string.error_invalid_number)
            }
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            binding.resultText.text = result
        }

        viewModel.rate.observe(viewLifecycleOwner) { rate ->
            binding.rateText.text = getString(R.string.current_rate_hint, rate)
            binding.convertBtn.isEnabled = (viewModel.loading.value != true)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.converterProgress.visibility = if (loading) View.VISIBLE else View.GONE
            binding.convertBtn.isEnabled = !loading && (viewModel.rate.value != null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}