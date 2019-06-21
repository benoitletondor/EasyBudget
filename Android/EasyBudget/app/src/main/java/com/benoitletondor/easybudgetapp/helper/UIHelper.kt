/*
 *   Copyright 2015 Benoit LETONDOR
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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import java.util.*
import kotlin.math.max

/**
 * Helper to manage compat with 5+
 *
 * @author Benoit LETONDOR
 */
object UIHelper {

    /**
     * Remove border of the button for Android 5+
     */
    fun removeButtonBorder(button: Button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.outlineProvider = null
        }
    }

    /**
     * Set the status bar color for Android 5+
     */
    fun setStatusBarColor(activity: Activity, @ColorRes colorRes: Int) {
        val window = activity.window

        if (window.attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS == 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        window.statusBarColor = ContextCompat.getColor(activity, colorRes)
    }

    /**
     * Check if the os version is compatible with activity enter animations (Android 5+) && the
     * activity contains the animation key
     */
    fun willAnimateActivityEnter(activity: Activity): Boolean {
        return activity.intent.getBooleanExtra(MainActivity.ANIMATE_TRANSITION_KEY, false)
    }

    /**
     * Animate activity enter if compatible
     */
    fun animateActivityEnter(activity: Activity, listener: Animator.AnimatorListener) {
        if (!willAnimateActivityEnter(activity)) {
            return
        }

        val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
        rootView.alpha = 0.0f

        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val viewTreeObserver = rootView.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // get the center for the clipping circle
                    val cx = activity.intent.getIntExtra(MainActivity.CENTER_X_KEY, rootView.width / 2)
                    val cy = activity.intent.getIntExtra(MainActivity.CENTER_Y_KEY, rootView.height / 2)

                    // get the final radius for the clipping circle
                    val finalRadius = max(rootView.width, rootView.height)

                    // create the animator for this view (the start radius is zero)
                    val anim = ViewAnimationUtils.createCircularReveal(rootView, cx, cy, 0f, finalRadius.toFloat())
                    anim.addListener(listener)
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            rootView.alpha = 1.0f
                        }
                    })
                    anim.start()
                }
            })
        }
    }

    /**
     * Set the focus on the given text view
     */
    fun setFocus(editText: EditText) {
        editText.requestFocus()

        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        Objects.requireNonNull(imm).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Show the FAB, animating the appearance if activated (the FAB should be configured with scale & alpha to 0)
     */
    fun showFAB(fab: View, parameters: Parameters) {
        if ( parameters.areAnimationsEnabled() ) {
            ViewCompat.animate(fab)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(AccelerateInterpolator())
                .withLayer()
                .start()
        } else {
            fab.scaleX = 1.0f
            fab.scaleY = 1.0f
            fab.alpha = 1.0f
        }
    }



    /**
     * This helper prevents the user to add unsupported values into an EditText for decimal numbers
     */
    fun preventUnsupportedInputForDecimals(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val value = editText.text.toString()

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
     * Center buttons of the given dialog (used to center when 3 choices are available).
     *
     * @param dialog the dialog
     */
    fun centerDialogButtons(dialog: AlertDialog) {
        try {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val positiveButtonLL = positiveButton.layoutParams as LinearLayout.LayoutParams
            positiveButtonLL.gravity = Gravity.CENTER
            positiveButton.layoutParams = positiveButtonLL

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val negativeButtonL = negativeButton.layoutParams as LinearLayout.LayoutParams
            negativeButtonL.gravity = Gravity.CENTER
            negativeButton.layoutParams = negativeButtonL

            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val neutralButtonL = neutralButton.layoutParams as LinearLayout.LayoutParams
            neutralButtonL.gravity = Gravity.CENTER
            neutralButton.layoutParams = neutralButtonL
        } catch (e: Exception) {
            Logger.error("Error while centering dialog buttons", e)
        }

    }
}

/**
 * Are animations enabled (boolean)
 */
private const val ANIMATIONS_ENABLED_PARAMETERS_KEY = "animation_enabled"

/**
 * Are animations enabled (can be disabled by user in settings)
 */
fun Parameters.areAnimationsEnabled(): Boolean {
    return getBoolean(ANIMATIONS_ENABLED_PARAMETERS_KEY, true)
}

/**
 * Set animation enabled value
 */
fun Parameters.setAnimationsEnabled(enabled: Boolean) {
    putBoolean(ANIMATIONS_ENABLED_PARAMETERS_KEY, enabled)
}