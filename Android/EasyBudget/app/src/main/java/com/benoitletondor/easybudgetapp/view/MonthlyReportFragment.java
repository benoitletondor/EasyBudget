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

package com.benoitletondor.easybudgetapp.view;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.db.DB;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportFragment extends Fragment
{
    private Date date;
    private List<Expense> expenses;
    private List<Expense> revenues;
    private List<Expense> monthlyExpenses;
    private List<Expense> monthlyRevenues;

    @SuppressLint("ValidFragment")
    public MonthlyReportFragment(@NonNull Date date)
    {
        this.date = date;
    }

    public MonthlyReportFragment()
    {
        throw new RuntimeException("You should not use this fragment in XML");
    }

// ---------------------------------->

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_monthly_report, container, false);

        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.monthly_report_fragment_progress_bar);
        final View content = v.findViewById(R.id.monthly_report_fragment_content);
        final View emptyState = v.findViewById(R.id.monthly_report_fragment_empty_state);

        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Void... params)
            {
                DB db = new DB(getActivity());
                try
                {
                    List<Expense> expensesForMonth = db.getExpensesForMonth(date);
                    if( expensesForMonth.isEmpty() )
                    {
                        return false;
                    }

                    expenses = new ArrayList<>();
                    revenues = new ArrayList<>();
                    monthlyExpenses = new ArrayList<>();
                    monthlyRevenues = new ArrayList<>();

                    for(Expense expense : expensesForMonth)
                    {
                        if( expense.isRevenue() )
                        {
                            if( expense.isMonthly() )
                            {
                                monthlyRevenues.add(expense);
                            }
                            else
                            {
                                revenues.add(expense);
                            }
                        }
                        else
                        {
                            if( expense.isMonthly() )
                            {
                                monthlyExpenses.add(expense);
                            }
                            else
                            {
                                expenses.add(expense);
                            }
                        }
                    }
                }
                finally
                {
                    db.close();
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean hasResults)
            {
                progressBar.setVisibility(View.GONE);

                if( hasResults )
                {
                    content.setVisibility(View.VISIBLE);
                }
                else
                {
                    emptyState.setVisibility(View.VISIBLE);
                }
            }
        }.execute();

        return v;
    }
}
