/*
 *   Copyright 2025 Benoit Letondor
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

package com.benoitletondor.easybudgetapp.helper

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import com.benoitletondor.easybudgetapp.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * This helper prevents the user to add unsupported values into an TextField for decimal numbers
 */
fun String.sanitizeFromUnsupportedInputForDecimals(supportsNegativeValue: Boolean = true): String {
    val s = Editable.Factory.getInstance().newEditable(
        filter { (if (supportsNegativeValue) "-0123456789.," else "0123456789.,").contains(it) }
    )

    s.sanitizeFromUnsupportedInputForDecimals()

    return s.toString()
}

fun EditText.preventUnsupportedInputForDecimals() {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            s.sanitizeFromUnsupportedInputForDecimals()
        }
    })
}

private fun Editable.sanitizeFromUnsupportedInputForDecimals() {
    try {
        // Remove - that is not at first char
        val minusIndex = lastIndexOf("-")
        if (minusIndex > 0) {
            delete(minusIndex, minusIndex + 1)

            if (startsWith("-")) {
                delete(0, 1)
            } else {
                insert(0, "-")
            }

            return
        }

        val comaIndex = indexOf(",")
        val dotIndex = indexOf(".")
        val lastDotIndex = lastIndexOf(".")

        // Remove ,
        if (comaIndex >= 0) {
            if (dotIndex >= 0) {
                delete(comaIndex, comaIndex + 1)
            } else {
                replace(comaIndex, comaIndex + 1, ".")
            }

            return
        }

        // Disallow double .
        if (dotIndex >= 0 && dotIndex != lastDotIndex) {
            delete(lastDotIndex, lastDotIndex + 1)
        } else if (dotIndex >= 0) {
            // No more than 2 decimals
            val decimals = substring(dotIndex + 1)
            if (decimals.length > 2) {
                delete(dotIndex + 3, length)
            }
        }
    } catch (e: Exception) {
        Logger.error("An error occurred during text changing watcher. Value: $this", e)
    }
}

/**
 * Center buttons of the given dialog (used to center when 3 choices are available).
 */
fun AlertDialog.centerButtons() {
    try {
        getButton(AlertDialog.BUTTON_POSITIVE)?.updateLayoutParams<LinearLayout.LayoutParams> {
            gravity = Gravity.CENTER
        }

        getButton(AlertDialog.BUTTON_NEGATIVE)?.updateLayoutParams<LinearLayout.LayoutParams> {
            gravity = Gravity.CENTER
        }

        getButton(AlertDialog.BUTTON_NEUTRAL)?.updateLayoutParams<LinearLayout.LayoutParams> {
            gravity = Gravity.CENTER
        }
    } catch (e: Exception) {
        Logger.error("Error while centering dialog buttons", e)
    }
}

/**
 * Get the title of the month to display in the report view
 *
 * @param context non null context
 * @return a formatted string like "January 2016"
 */
fun YearMonth.getMonthTitle(context: Context): String {
    val format = DateTimeFormatter.ofPattern(context.resources.getString(R.string.monthly_report_month_title_format), Locale.getDefault())
    return format.format(this)
}
