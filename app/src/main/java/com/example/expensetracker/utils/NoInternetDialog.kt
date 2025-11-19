package com.example.expensetracker.utils

import android.app.AlertDialog
import android.content.Context

/**
 * Shows a simple "No Internet Connection" dialog with Retry and Close buttons
 */
object NoInternetDialog {

    fun show(
        context: Context,
        onRetry: () -> Unit,
        onClose: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                onRetry()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
                onClose?.invoke()
            }
            .setCancelable(false)
            .show()
    }
}

