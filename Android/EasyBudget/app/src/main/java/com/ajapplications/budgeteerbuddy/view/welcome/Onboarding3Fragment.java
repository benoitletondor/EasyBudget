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


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.helper.Logger;
import com.ajapplications.budgeteerbuddy.helper.UIHelper;
import com.ajapplications.budgeteerbuddy.model.Expense;
import com.ajapplications.budgeteerbuddy.model.db.DB;
import com.ajapplications.budgeteerbuddy.R;

import java.util.Date;

/**
 * Onboarding step 3 fragment
 *
 * @author Benoit LETONDOR
 */
public class Onboarding3Fragment extends OnboardingFragment
{
    private TextView moneyTextView;
    private EditText amountEditText;
    private Button nextButton;

// -------------------------------------->

    /**
     * Required empty public constructor
     */
    public Onboarding3Fragment()
    {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_onboarding3, container, false);

        DB db = getDB();

        double amount = 0;
        if( db != null )
        {
            amount = -db.getBalanceForDay(new Date());
        }

        moneyTextView = (TextView) v.findViewById(R.id.onboarding_screen3_initial_amount_money_tv);
        setCurrency();

        amountEditText = (EditText) v.findViewById(R.id.onboarding_screen3_initial_amount_et);
        amountEditText.setText(amount == 0 ? "0" : String.valueOf(amount));
        UIHelper.preventUnsupportedInputForDecimals(amountEditText);
        amountEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                setButtonText();
            }
        });

        nextButton = (Button) v.findViewById(R.id.onboarding_screen3_next_button);
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DB db = getDB();
                if (db != null)
                {
                    double currentBalance = -db.getBalanceForDay(new Date());
                    double newBalance = getAmountValue();

                    if (newBalance != currentBalance)
                    {
                        double diff = newBalance - currentBalance;

                        final Expense expense = new Expense(getResources().getString(R.string.adjust_balance_expense_title), -diff, new Date());
                        db.persistExpense(expense);
                    }
                }

                // Hide keyboard
                try
                {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(amountEditText.getWindowToken(), 0);
                }
                catch(Exception e)
                {
                    Logger.error("Error while hiding keyboard", e);
                }

                next(v);
            }
        });
        setButtonText();

        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        if( isVisibleToUser ) // Update values on display
        {
            setCurrency();
            setButtonText();
        }
    }

    @Override
    public int getStatusBarColor()
    {
        return R.color.secondary_dark;
    }

// -------------------------------------->

    private void setCurrency()
    {
        if( moneyTextView != null ) // Will be null if view is not yet created
        {
            moneyTextView.setText(CurrencyHelper.getUserCurrency(getActivity()).getSymbol());
        }
    }

    private double getAmountValue()
    {
        String valueString = amountEditText.getText().toString();

        try
        {
            return ("".equals(valueString) || "-".equals(valueString)) ? 0 : Double.valueOf(valueString);
        }
        catch (Exception e)
        {
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.adjust_balance_error_title)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .show();

            Logger.warning("An error occurred during initial amount parsing: "+valueString, e);
            return 0;
        }
    }

    private void setButtonText()
    {
        if( nextButton != null )
        {
            double value = getAmountValue();

            nextButton.setText(getActivity().getString(R.string.onboarding_screen_3_cta, CurrencyHelper.getFormattedCurrencyString(getActivity(), value)));
        }
    }
}
