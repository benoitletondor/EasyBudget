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

package com.benoitletondor.easybudgetapp.helper

import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.benoitletondor.easybudgetapp.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * This helper prevents the user to add unsupported values into an EditText for decimal numbers
 */
fun EditText.preventUnsupportedInputForDecimals() {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            val value = text.toString()

            try {
                // Remove - that is not at first char
                val minusIndex = value.lastIndexOf("-")
                if (minusIndex > 0) {
                    s.delete(minusIndex, minusIndex + 1)

                    if (value.startsWith("-")) {
                        s.delete(0, 1)
                    } else {
                        s.insert(0, "-")
                    }

                    return
                }

                val comaIndex = value.indexOf(",")
                val dotIndex = value.indexOf(".")
                val lastDotIndex = value.lastIndexOf(".")

                // Remove ,
                if (comaIndex >= 0) {
                    if (dotIndex >= 0) {
                        s.delete(comaIndex, comaIndex + 1)
                    } else {
                        s.replace(comaIndex, comaIndex + 1, ".")
                    }

                    return
                }

                // Disallow double .
                if (dotIndex >= 0 && dotIndex != lastDotIndex) {
                    s.delete(lastDotIndex, lastDotIndex + 1)
                } else if (dotIndex > 0) {
                    val decimals = value.substring(dotIndex + 1)
                    if (decimals.length > 2) {
                        s.delete(dotIndex + 3, value.length)
                    }
                }// No more than 2 decimals
            } catch (e: Exception) {
                Logger.error("An error occurred during text changing watcher. Value: $value", e)
            }
        }
    })
}

/**
 * Show the FAB, animating the appearance if activated (the FAB should be configured with scale & alpha to 0)
 */
fun View.animateFABAppearance() {
    ViewCompat.animate(this)
        .scaleX(1.0f)
        .scaleY(1.0f)
        .alpha(1.0f)
        .setInterpolator(AccelerateInterpolator())
        .withLayer()
        .start()
}

/**
 * Set the focus on the given text view
 */
fun EditText.setFocus() {
    requestFocus()

    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Set the status bar color
 */
fun Activity.setStatusBarColor(@ColorRes colorRes: Int) {
    val window = window

    if (window.attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS == 0) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    window.statusBarColor = ContextCompat.getColor(this, colorRes)

    if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
        window.navigationBarColor = ContextCompat.getColor(this, colorRes)
    }
}

fun Activity.setNavigationBarColored() {
    var flags = window.decorView.systemUiVisibility
    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

    window.decorView.systemUiVisibility = flags
}

/**
 * Remove border of the button
 */
fun Button.removeButtonBorder() {
    outlineProvider = null
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
