package com.benoitletondor.easybudgetapp.view.main;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

/**
 * Behavior of FAB for {@link FloatingActionsMenu} reacting to Snackbar
 *
 * @author Benoit LETONDOR
 */
public class FloatingActionButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionsMenu>
{
    /**
     * Constructor to make it inflatable from XML
     *
     * @param context
     * @param attrs
     */
    public FloatingActionButtonBehavior(Context context, AttributeSet attrs)
    {}

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionsMenu child, View dependency)
    {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionsMenu child, View dependency)
    {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(translationY);
        return true;
    }
}
