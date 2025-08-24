package com.example.expensetracker.AppScreens.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class TransactionAdapter(
    private val transactions: List<TransactionResponse>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.transaction_icon)
        val title: TextView = itemView.findViewById(R.id.transaction_title)
        val category: TextView = itemView.findViewById(R.id.transaction_category)
        val amount: TextView = itemView.findViewById(R.id.money_amount)
        val date: TextView = itemView.findViewById(R.id.transaction_date)
        val deleteBtn: AppCompatButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recent_transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.title.text = transaction.title
        holder.category.text = transaction.category
        // Parse UTC date and format
        val formattedDate = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC") // API is in UTC
            val date: Date = parser.parse(transaction.created_at) ?: Date()
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.format(date) // formatted in local timezone
        } catch (e: Exception) {
            transaction.created_at.take(10) // fallback
        }

        holder.date.text = formattedDate

        // Amount formatting
        val amountValue = transaction.amount.toDoubleOrNull() ?: 0.0
        if (transaction.category.lowercase() == "income") {
            holder.amount.text = "+$amountValue"
            holder.amount.setTextColor(holder.itemView.context.getColor(R.color.income))
            holder.icon.setImageResource(R.drawable.arrow_circle_up_24px)
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.income))
        } else {
            holder.amount.text = "-$amountValue"
            holder.amount.setTextColor(holder.itemView.context.getColor(R.color.expense))
            holder.icon.setImageResource(R.drawable.arrow_circle_down_24px)
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.expense))
        }

        // Delete button (for now just log/Toast)
        holder.deleteBtn.setOnClickListener {
            // TODO: implement delete later
        }
    }

    override fun getItemCount(): Int = transactions.size
}