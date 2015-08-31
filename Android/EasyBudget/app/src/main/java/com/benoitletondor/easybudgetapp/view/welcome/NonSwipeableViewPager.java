package com.benoitletondor.easybudgetapp.view.welcome;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * A {@link ViewPager} that doesn't allow manual swiping
 *
 * @author Benoit LETONDOR
 */
public class NonSwipeableViewPager extends ViewPager
{
    public NonSwipeableViewPager(Context context)
    {
        super(context);
    }

    public NonSwipeableViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event)
    {
        // Never allow swiping by keyboard LEFT, RIGHT, TAB and SHIFT+TAB keys
        return false;
    }
}