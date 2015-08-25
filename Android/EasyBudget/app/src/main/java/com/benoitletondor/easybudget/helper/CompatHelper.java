package com.benoitletondor.easybudget.helper;

import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Button;

/**
 * Helper to manage compat with 5+
 *
 * @author Benoit LETONDOR
 */
public class CompatHelper
{
    /**
     * Remove border of the button for android 5+
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
}
