package com.benoitletondor.easybudgetapp.view.settings.subviews

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.benoitletondor.easybudgetapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.showLowMoneyWarningAmountPickerDialog(
    lowMoneyWarningAmount: Int,
    onLowMoneyWarningAmountChanged: (Int) -> Unit,
) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_warning_limit, null)
    val limitEditText = dialogView?.findViewById<View>(R.id.warning_limit) as EditText
    limitEditText.setText(lowMoneyWarningAmount.toString())
    limitEditText.setSelection(limitEditText.text.length) // Put focus at the end of the text

    val builder = MaterialAlertDialogBuilder(this)
    builder.setTitle(R.string.adjust_limit_warning_title)
    builder.setMessage(R.string.adjust_limit_warning_message)
    builder.setView(dialogView)
    builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
    builder.setPositiveButton(R.string.ok) { _, _ ->
        var limitString = limitEditText.text.toString()
        if (limitString.trim { it <= ' ' }.isEmpty()) {
            limitString = "0" // Set a 0 value if no value is provided (will lead to an error displayed to the user)
        }

        try {
            val newLimit = Integer.valueOf(limitString)

            // Invalid value, alert the user
            if (newLimit <= 0) {
                throw IllegalArgumentException("limit should be > 0")
            }

            onLowMoneyWarningAmountChanged(newLimit)
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.adjust_limit_warning_error_title)
                .setMessage(resources.getString(R.string.adjust_limit_warning_error_message))
                .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        }
    }

    val dialog = builder.show()

    // Directly show keyboard when the dialog pops
    limitEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        // Check if the device doesn't have a physical keyboard
        if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }
}