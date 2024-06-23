/*
 *   Copyright 2024 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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
        if (hasFocus) {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }
    limitEditText.requestFocus()
}