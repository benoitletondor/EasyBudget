package com.benoitletondor.easybudgetapp.helper;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.benoitletondor.easybudgetapp.view.MainActivity;

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
    public static boolean willAnimateActivityEnter()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Animate activity enter if compatible and then focus the given edit text
     *
     * @param activity
     * @param focusText
     */
    public static void animateActivityEnterThenFocus(@NonNull final Activity activity, @NonNull final EditText focusText)
    {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !activity.getIntent().getBooleanExtra(MainActivity.ANIMATE_TRANSITION_KEY, false))
        {
            setFocus(focusText);
            return;
        }

        final ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
        if ( viewTreeObserver.isAlive() )
        {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
            {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onGlobalLayout()
                {
                    // get the center for the clipping circle
                    int cx = activity.getIntent().getIntExtra(MainActivity.CENTER_X_KEY, activity.getWindow().getDecorView().getWidth() / 2);
                    int cy = activity.getIntent().getIntExtra(MainActivity.CENTER_Y_KEY, activity.getWindow().getDecorView().getHeight() / 2);

                    // get the final radius for the clipping circle
                    int finalRadius = Math.max(activity.getWindow().getDecorView().getWidth(), activity.getWindow().getDecorView().getHeight());

                    // create the animator for this view (the start radius is zero)
                    Animator anim = ViewAnimationUtils.createCircularReveal(activity.getWindow().getDecorView(), cx, cy, 0, finalRadius);
                    anim.addListener(new Animator.AnimatorListener()
                    {
                        @Override
                        public void onAnimationStart(Animator animation)
                        {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            UIHelper.setFocus(focusText);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation)
                        {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation)
                        {

                        }
                    });
                    anim.start();

                    activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
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
}
