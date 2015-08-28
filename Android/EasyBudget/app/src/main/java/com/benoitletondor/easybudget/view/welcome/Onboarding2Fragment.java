package com.benoitletondor.easybudget.view.welcome;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.benoitletondor.easybudget.R;

/**
 * Onboarding step 1 fragment
 *
 * @author Benoit LETONDOR
 */
public class Onboarding2Fragment extends Fragment
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
        return inflater.inflate(R.layout.fragment_onboarding2, container, false);
    }


}
