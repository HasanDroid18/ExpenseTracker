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
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class TransactionAdapter(
    private val transactions: List<TransactionResponse>,
    private val onDelete: (TransactionResponse) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_ITEM = 1
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.transaction_icon)
        val title: TextView = itemView.findViewById(R.id.transaction_title)
        val category: TextView = itemView.findViewById(R.id.transaction_category)
        val amount: TextView = itemView.findViewById(R.id.money_amount)
        val date: TextView = itemView.findViewById(R.id.transaction_date)
        val deleteBtn: AppCompatButton = itemView.findViewById(R.id.deleteButton)
    }

    inner class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int = if (transactions.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_EMPTY) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_empty_state, parent, false)
            EmptyViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_transaction_item, parent, false)
            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is EmptyViewHolder) return
        holder as TransactionViewHolder

        val transaction = transactions[position]

        holder.title.text = transaction.title
        holder.category.text = transaction.category

        val formattedDate = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date: Date = parser.parse(transaction.created_at) ?: Date()
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            transaction.created_at.take(10)
        }
        holder.date.text = formattedDate

        val amountValue = transaction.amount
        if (transaction.category.lowercase() == "income") {
            holder.amount.text = String.format(Locale.getDefault(), "+%.2f", amountValue)
            holder.amount.setTextColor(holder.itemView.context.getColor(R.color.income))
            holder.icon.setImageResource(R.drawable.arrow_circle_up_24px)
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.income))
        } else {
            holder.amount.text = String.format(Locale.getDefault(), "-%.2f", amountValue)
            holder.amount.setTextColor(holder.itemView.context.getColor(R.color.expense))
            holder.icon.setImageResource(R.drawable.arrow_circle_down_24px)
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.expense))
        }

        holder.deleteBtn.setOnClickListener { onDelete(transaction) }
    }

    override fun getItemCount(): Int = if (transactions.isEmpty()) 1 else transactions.size
}