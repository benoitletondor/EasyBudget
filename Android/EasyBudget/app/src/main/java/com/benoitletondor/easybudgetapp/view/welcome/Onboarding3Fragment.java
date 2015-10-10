package com.benoitletondor.easybudgetapp.view.welcome;


import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.db.DB;

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

        int amount = 0;
        if( db != null )
        {
            amount = db.getBalanceForDay(new Date());
        }

        moneyTextView = (TextView) v.findViewById(R.id.onboarding_screen3_initial_amount_money_tv);
        setCurrency();

        amountEditText = (EditText) v.findViewById(R.id.onboarding_screen3_initial_amount_et);
        amountEditText.setText(String.valueOf(amount));
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
                    int currentBalance = db.getBalanceForDay(new Date());
                    int newBalance = Integer.valueOf(amountEditText.getText().toString());

                    if (newBalance == currentBalance)
                    {
                        // Nothing to do, balance hasn't change
                        return;
                    }

                    int diff = newBalance - currentBalance;

                    final Expense expense = new Expense(getResources().getString(R.string.adjust_balance_expense_title), -diff, new Date());
                    db.persistExpense(expense);
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
        return R.color.onboarding_3_statusbar;
    }

// -------------------------------------->

    private void setCurrency()
    {
        if( moneyTextView != null ) // Will be null if view is not yet created
        {
            moneyTextView.setText(CurrencyHelper.getUserCurrency(getActivity()).getSymbol());
        }
    }

    private void setButtonText()
    {
        if( nextButton != null )
        {
            String valueString = amountEditText.getText().toString();
            int value = "".equals(valueString) ? 0 : Integer.valueOf(valueString);

            nextButton.setText(getActivity().getString(R.string.onboarding_start_with_amount, CurrencyHelper.getFormattedCurrencyString(getActivity(), value)));
        }
    }
}
