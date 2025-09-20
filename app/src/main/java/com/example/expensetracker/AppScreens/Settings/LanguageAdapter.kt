package com.example.expensetracker.AppScreens.Settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.databinding.ItemLanguageBinding

data class Language(
    val code: String,
    val name: String,
    val isSelected: Boolean = false
)

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = languages.indexOfFirst { it.isSelected }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language, position: Int) {
            binding.tvLanguageName.text = language.name
            binding.ivSelected.visibility = if (position == selectedPosition) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position

                // Update the UI
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                // Notify the callback
                onLanguageSelected(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position)
    }

    override fun getItemCount(): Int = languages.size
}
