/*
 *   Copyright 2016 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.report;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.db.DB;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportFragment extends Fragment
{
    /**
     * The first date of the month at 00:00:00
     */
    @NonNull
    private final Date date;
    /**
     * The computed revenue amount (always >= 0)
     */
    private double revenuesAmount = 0d;
    /**
     * The computed expenses amount (always >= 0)
     */
    private double expensesAmount = 0d;

// ---------------------------------->

    @SuppressLint("ValidFragment")
    public MonthlyReportFragment(@NonNull Date date)
    {
        this.date = date;
    }

    public MonthlyReportFragment()
    {
        // This is just in case the fragment get instanciated by the OS after activity got killed...
        // This will probably lead to a badly configured fragment but it's better than a crash...
        // I guess :/
        // FIXME find a better solution!
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        date = cal.getTime();
    }

// ---------------------------------->

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Reset totals
        expensesAmount = 0.0d;
        revenuesAmount = 0.0d;

        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_monthly_report, container, false);

        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.monthly_report_fragment_progress_bar);
        final View content = v.findViewById(R.id.monthly_report_fragment_content);
        final RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.monthly_report_fragment_recycler_view);
        final View emptyState = v.findViewById(R.id.monthly_report_fragment_empty_state);
        final TextView revenuesAmountTextView = (TextView) v.findViewById(R.id.monthly_report_fragment_revenues_total_tv);
        final TextView expensesAmountTextView = (TextView) v.findViewById(R.id.monthly_report_fragment_expenses_total_tv);
        final TextView balanceTextView = (TextView) v.findViewById(R.id.monthly_report_fragment_balance_tv);

        new AsyncTask<Void, Void, MonthlyReportRecyclerViewAdapter>()
        {
            @Override
            protected MonthlyReportRecyclerViewAdapter doInBackground(Void... params)
            {
                final DB db = new DB(getActivity());
                try
                {
                    List<Expense> expensesForMonth = db.getExpensesForMonth(date);
                    if( expensesForMonth.isEmpty() )
                    {
                        return null;
                    }

                    final List<Expense> expenses = new ArrayList<>();
                    final List<Expense> revenues = new ArrayList<>();

                    for(Expense expense : expensesForMonth)
                    {
                        if( expense.isRevenue() )
                        {
                            revenues.add(expense);
                            revenuesAmount -= expense.getAmount();
                        }
                        else
                        {
                            expenses.add(expense);
                            expensesAmount += expense.getAmount();
                        }
                    }

                    return new MonthlyReportRecyclerViewAdapter(expenses, revenues);
                }
                finally
                {
                    db.close();
                }
            }

            @Override
            protected void onPostExecute(MonthlyReportRecyclerViewAdapter adapter)
            {
                progressBar.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);

                if( adapter != null )
                {
                    configureRecyclerView(recyclerView, adapter);
                }
                else
                {
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                }

                configureTotalView(revenuesAmountTextView, expensesAmountTextView, balanceTextView);
            }
        }.execute();

        return v;
    }

    /**
     * Configure recycler view LayoutManager & adapter
     *
     * @param recyclerView
     * @param adapter
     */
    private void configureRecyclerView(@NonNull RecyclerView recyclerView, @NonNull MonthlyReportRecyclerViewAdapter adapter)
    {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configure textviews for expenses, revenues & balance
     *
     * @param revenuesAmountTextView
     * @param expensesAmountTextView
     * @param balanceTextView
     */
    private void configureTotalView(@NonNull TextView revenuesAmountTextView, @NonNull TextView expensesAmountTextView, @NonNull TextView balanceTextView)
    {
        revenuesAmountTextView.setText(CurrencyHelper.getFormattedCurrencyString(revenuesAmountTextView.getContext(), revenuesAmount));
        expensesAmountTextView.setText(CurrencyHelper.getFormattedCurrencyString(expensesAmountTextView.getContext(), expensesAmount));

        double balance = revenuesAmount - expensesAmount;
        balanceTextView.setText(CurrencyHelper.getFormattedCurrencyString(balanceTextView.getContext(), balance));
        balanceTextView.setTextColor(ContextCompat.getColor(balanceTextView.getContext(), balance >= 0 ? R.color.budget_green : R.color.budget_red));
    }
}
