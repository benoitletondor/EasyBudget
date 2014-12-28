package com.benoitletondor.easybudget.helper;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * EditText that keeps padding after background is set on Android 4.X.<br>
 * See <a href="http://stackoverflow.com/questions/10095196/whered-padding-go-when-setting-background-drawable">Stackoverflow</a>
 *
 * @author Benoit LETONDOR
 */
public class PaddingEditText extends EditText
{
    public PaddingEditText(Context context)
    {
        super(context);
    }

    public PaddingEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PaddingEditText(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PaddingEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

// --------------------------------------->

    @Override
    public void setBackgroundResource(int resid)
    {
        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int pr = getPaddingRight();
        int pb = getPaddingBottom();

        super.setBackgroundResource(resid);

        this.setPadding(pl, pt, pr, pb);
    }
}
