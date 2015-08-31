package com.benoitletondor.easybudgetapp.view.welcome;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.benoitletondor.easybudgetapp.R;


public class Onboarding3Fragment extends OnboardingFragment
{

    public Onboarding3Fragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_onboarding3, container, false);

        v.findViewById(R.id.onboarding_screen3_next_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                done();
            }
        });

        return v;
    }

    @Override
    public int getStatusBarColor()
    {
        return R.color.onboarding_3_statusbar;
    }
}
