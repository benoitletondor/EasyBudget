package com.benoitletondor.easybudgetapp.helper;

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
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
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

        final View rootView;

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) // Animating the decor view doesn't work anymore on M, take the content view
        {
            rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
            rootView.setAlpha(0.0f);

            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        else
        {
            rootView = activity.getWindow().getDecorView();

            activity.overridePendingTransition(0, 0);
        }

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
}
