package com.benoitletondor.easybudget.view.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
public class Onboarding1Fragment extends Fragment
{
    /**
     * Required empty public constructor
     */
    public Onboarding1Fragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_onboarding1, container, false);

        v.findViewById(R.id.onboarding_screen1_next_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(WelcomeActivity.PAGER_NEXT_INTENT);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        });

        return v;
    }
}
