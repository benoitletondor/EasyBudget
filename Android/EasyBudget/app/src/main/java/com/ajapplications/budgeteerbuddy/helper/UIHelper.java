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

package com.ajapplications.budgeteerbuddy.helper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ajapplications.budgeteerbuddy.view.MainActivity;

/**
 * Helper to manage compat with 5+
 *
 * @author Benoit LETONDOR
 */
public class UIHelper
{
    /**
     * Remove border of the button for Android 5+
     *
     * @param button
     */
    public static void removeButtonBorder(@NonNull Button button)
    {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            button.setOutlineProvider(null);
        }
    }

    /**
     * Set the status bar color for Android 5+
     *
     * @param activity
     * @param colorRes
     */
    public static void setStatusBarColor(@NonNull Activity activity, @ColorRes int colorRes)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Window window = activity.getWindow();

            if( (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) == 0 )
            {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }

            window.setStatusBarColor(ContextCompat.getColor(activity, colorRes));
        }
    }

    /**
     * Check if the os version is compatible with activity enter animations (Android 5+)
     *
     * @return
     */
    public static boolean isCompatibleWithActivityEnterAnimation()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Check if the os version is compatible with activity enter animations (Android 5+) && the
     * activity contains the animation key
     *
     * @return
     */
    public static boolean willAnimateActivityEnter(Activity activity)
    {
        return isCompatibleWithActivityEnterAnimation() && activity.getIntent().getBooleanExtra(MainActivity.ANIMATE_TRANSITION_KEY, false);
    }

    /**
     * Animate activity enter if compatible
     *
     * @param activity
     * @param listener
     */
    public static void animateActivityEnter(@NonNull final Activity activity, @NonNull final Animator.AnimatorListener listener)
    {
        if( !willAnimateActivityEnter(activity) )
        {
            return;
        }

        final View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.setAlpha(0.0f);

        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        final ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        if ( viewTreeObserver.isAlive() )
        {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
            {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onGlobalLayout()
                {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // get the center for the clipping circle
                    int cx = activity.getIntent().getIntExtra(MainActivity.CENTER_X_KEY, rootView.getWidth() / 2);
                    int cy = activity.getIntent().getIntExtra(MainActivity.CENTER_Y_KEY, rootView.getHeight() / 2);

                    // get the final radius for the clipping circle
                    int finalRadius = Math.max(rootView.getWidth(), rootView.getHeight());

                    // create the animator for this view (the start radius is zero)
                    Animator anim = ViewAnimationUtils.createCircularReveal(rootView, cx, cy, 0, finalRadius);
                    anim.addListener(listener);
                    anim.addListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationStart(Animator animation)
                        {
                            rootView.setAlpha(1.0f);
                        }
                    });
                    anim.start();
                }
            });
        }
    }

    /**
     * Set the focus on the given text view
     *
     * @param editText
     */
    public static void setFocus(@NonNull EditText editText)
    {
        editText.requestFocus();

        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Show the FAB, animating the appearance if activated (the FAB should be configured with scale & alpha to 0)
     *
     * @param fab
     */
    public static void showFAB(@NonNull final View fab)
    {
        if( UIHelper.areAnimationsEnabled(fab.getContext()) )
        {
            ViewCompat.animate(fab)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(new AccelerateInterpolator())
                .withLayer()
                .start();
        }
        else
        {
            fab.setScaleX(1.0f);
            fab.setScaleY(1.0f);
            fab.setAlpha(1.0f);
        }
    }

    /**
     * Are animations enabled (can be disabled by user in settings)
     *
     * @param context
     * @return
     */
    public static boolean areAnimationsEnabled(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.ANIMATIONS_ENABLED, true);
    }

    /**
     * Set animation enabled value
     *
     * @param context
     * @param enabled
     */
    public static void setAnimationsEnabled(@NonNull Context context, boolean enabled)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.ANIMATIONS_ENABLED, enabled);
    }

    /**
     * This helper prevents the user to add unsupported values into an EditText for decimal numbers
     *
     * @param editText
     */
    public static void preventUnsupportedInputForDecimals(final @NonNull EditText editText)
    {
        editText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                String value = editText.getText().toString();

                try
                {
                    // Remove - that is not at first char
                    int minusIndex = value.lastIndexOf("-");
                    if (minusIndex > 0)
                    {
                        s.delete(minusIndex, minusIndex + 1);

                        if (value.startsWith("-"))
                        {
                            s.delete(0, 1);
                        }
                        else
                        {
                            s.insert(0, "-");
                        }

                        return;
                    }

                    int comaIndex = value.indexOf(",");
                    int dotIndex = value.indexOf(".");
                    int lastDotIndex = value.lastIndexOf(".");

                    // Remove ,
                    if (comaIndex >= 0)
                    {
                        if (dotIndex >= 0)
                        {
                            s.delete(comaIndex, comaIndex + 1);
                        }
                        else
                        {
                            s.replace(comaIndex, comaIndex + 1, ".");
                        }

                        return;
                    }

                    // Disallow double .
                    if (dotIndex >= 0 && dotIndex != lastDotIndex)
                    {
                        s.delete(lastDotIndex, lastDotIndex + 1);
                    }
                    // No more than 2 decimals
                    else if (dotIndex > 0)
                    {
                        String decimals = value.substring(dotIndex + 1);
                        if (decimals.length() > 2)
                        {
                            s.delete(dotIndex + 3, value.length());
                        }
                    }
                }
                catch (Exception e)
                {
                    Logger.error("An error occurred during text changing watcher. Value: " + value, e);
                }
            }
        });
    }

    /**
     * Center buttons of the given dialog (used to center when 3 choices are available).
     *
     * @param dialog the dialog
     */
    public static void centerDialogButtons(@NonNull AlertDialog dialog)
    {
        try
        {
            final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
            positiveButtonLL.gravity = Gravity.CENTER;
            positiveButton.setLayoutParams(positiveButtonLL);

            final Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            LinearLayout.LayoutParams negativeButtonL = (LinearLayout.LayoutParams) negativeButton.getLayoutParams();
            negativeButtonL.gravity = Gravity.CENTER;
            negativeButton.setLayoutParams(negativeButtonL);

            final Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            LinearLayout.LayoutParams neutralButtonL = (LinearLayout.LayoutParams) neutralButton.getLayoutParams();
            neutralButtonL.gravity = Gravity.CENTER;
            neutralButton.setLayoutParams(neutralButtonL);
        }
        catch (Exception e)
        {
            Logger.error("Error while centering dialog buttons", e);
        }
    }
}
