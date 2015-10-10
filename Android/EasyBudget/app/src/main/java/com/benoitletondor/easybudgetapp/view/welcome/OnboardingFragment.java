package com.benoitletondor.easybudgetapp.view.welcome;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.benoitletondor.easybudgetapp.model.db.DB;
import com.benoitletondor.easybudgetapp.view.WelcomeActivity;

/**
 * Abstract fragment that contains common methods of all onboarding fragments
 *
 * @author Benoit LETONDOR
 */
public abstract class OnboardingFragment extends Fragment
{
    /**
     * Get a DB connexion if available
     *
     * @return a db connexion if available, which will always be the case if we are in the Welcome Activity
     */
    @Nullable
    protected DB getDB()
    {
        FragmentActivity activity = getActivity();
        if( activity instanceof WelcomeActivity )
        {
            return ((WelcomeActivity) activity).getDB();
        }

        return null;
    }

    /**
     * Go to the next onboarding step without animation
     */
    protected void next()
    {
        Intent intent = new Intent(WelcomeActivity.PAGER_NEXT_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    /**
     * Go to the next onboarding step with a reveal animation starting from the given center
     *
     * @param animationCenter center of the reveal animation
     */
    protected void next(@NonNull View animationCenter)
    {
        Intent intent = new Intent(WelcomeActivity.PAGER_NEXT_INTENT);
        intent.putExtra(WelcomeActivity.ANIMATE_TRANSITION_KEY, true);
        intent.putExtra(WelcomeActivity.CENTER_X_KEY, (int) animationCenter.getX() + animationCenter.getWidth() / 2);
        intent.putExtra(WelcomeActivity.CENTER_Y_KEY, (int) animationCenter.getY() + animationCenter.getHeight() / 2);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    /**
     * Finish the onboarding flow
     */
    protected void done()
    {
        Intent intent = new Intent(WelcomeActivity.PAGER_DONE_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    /**
     * Get the status bar color that should be used for this fragment
     *
     * @return the wanted color of the status bar
     */
    @ColorRes
    public abstract int getStatusBarColor();
}
