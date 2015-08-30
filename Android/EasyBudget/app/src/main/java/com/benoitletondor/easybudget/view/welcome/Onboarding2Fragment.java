package com.benoitletondor.easybudget.view.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.view.WelcomeActivity;

/**
 * Onboarding step 1 fragment
 *
 * @author Benoit LETONDOR
 */
public class Onboarding2Fragment extends OnboardingFragment
{

    /**
     * Required empty public constructor
     */
    public Onboarding2Fragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_onboarding2, container, false);

        return v;
    }

    @Override
    public int getStatusBarColor()
    {
        return R.color.onboarding_2_statusbar;
    }
}
