package com.example.expensetracker.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment

/**
 * KeyboardUtils - Utility functions for keyboard management
 *
 * Features:
 * - Hide keyboard when tapping outside EditText
 * - Programmatically show/hide keyboard
 * - Setup touch listeners for auto-dismiss
 */
object KeyboardUtils {

    /**
     * Hide the soft keyboard
     *
     * @param activity The activity context
     */
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Hide the soft keyboard from a specific view
     *
     * @param view The view to hide keyboard from
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Hide the soft keyboard in a fragment
     *
     * @param fragment The fragment context
     */
    fun hideKeyboard(fragment: Fragment) {
        fragment.activity?.let { hideKeyboard(it) }
    }

    /**
     * Show the soft keyboard for a specific view
     *
     * @param view The view to show keyboard for (usually an EditText)
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Setup touch listener on a view to hide keyboard when touched
     * This is useful for parent layouts to dismiss keyboard when user taps outside input fields
     *
     * @param view The parent view to attach listener to
     * @param activity The activity context
     */
    fun setupHideKeyboardOnTouch(view: View, activity: Activity) {
        // Set up touch listener for non-text box views to hide keyboard
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard(activity)
                false // Return false to allow other touch events to proceed
            }
        }
    }

    /**
     * Setup touch listener on a view to hide keyboard when touched (Fragment version)
     *
     * @param view The parent view to attach listener to
     * @param fragment The fragment context
     */
    fun setupHideKeyboardOnTouch(view: View, fragment: Fragment) {
        fragment.activity?.let { activity ->
            setupHideKeyboardOnTouch(view, activity)
        }
    }

    /**
     * Setup keyboard dismiss for an entire view hierarchy
     * Recursively sets up touch listeners for all non-EditText views
     *
     * @param view The root view to start from
     * @param activity The activity context
     */
    fun setupHideKeyboardOnTouchRecursive(view: View, activity: Activity) {
        // If the view is not an EditText, set up the touch listener
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard(activity)
                false
            }
        }

        // If the view is a ViewGroup, recursively set up all children
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupHideKeyboardOnTouchRecursive(innerView, activity)
            }
        }
    }

    /**
     * Check if the soft keyboard is currently visible
     *
     * @param activity The activity context
     * @return true if keyboard is visible, false otherwise
     */
    fun isKeyboardVisible(activity: Activity): Boolean {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.isAcceptingText
    }
}

