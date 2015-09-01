package com.benoitletondor.easybudgetapp.helper;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Helper to manage compat with 5+
 *
 * @author Benoit LETONDOR
 */
public class CompatHelper
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
}
