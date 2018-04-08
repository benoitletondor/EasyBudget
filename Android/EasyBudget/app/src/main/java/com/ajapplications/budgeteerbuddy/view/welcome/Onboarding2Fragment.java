/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ajapplications.budgeteerbuddy.view.welcome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.view.selectcurrency.SelectCurrencyFragment;
import com.ajapplications.budgeteerbuddy.R;

import java.util.Currency;

/**
 * Onboarding step 2 fragment
 *
 * @author Benoit LETONDOR
 */
public class Onboarding2Fragment extends OnboardingFragment
{
    private Currency selectedCurrency;
    private Button nextButton;

    private BroadcastReceiver receiver;

// ------------------------------------->

    /**
     * Required empty public constructor
     */
    public Onboarding2Fragment()
    {

    }

// ------------------------------------->

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_onboarding2, container, false);

        nextButton = (Button) v.findViewById(R.id.onboarding_screen2_next_button);
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                next();
            }
        });

        selectedCurrency = CurrencyHelper.getUserCurrency(v.getContext());
        setNextButtonText();

        Fragment selectCurrencyFragment = new SelectCurrencyFragment();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.expense_select_container, selectCurrencyFragment).commit();

        IntentFilter filter = new IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                selectedCurrency = Currency.getInstance(intent.getStringExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA));
                setNextButtonText();
            }
        };

        LocalBroadcastManager.getInstance(v.getContext()).registerReceiver(receiver, filter);

        return v;
    }

    @Override
    public void onDestroyView()
    {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);

        super.onDestroyView();
    }

    @Override
    public int getStatusBarColor()
    {
        return R.color.secondary_dark;
    }

    /**
     * Set the next button text according to the selected currency
     */
    private void setNextButtonText()
    {
        nextButton.setText(getResources().getString(R.string.onboarding_screen_2_cta, selectedCurrency.getSymbol()));
    }
}
