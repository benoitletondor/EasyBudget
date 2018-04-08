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

package com.ajapplications.budgeteerbuddy.view.report;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.model.Expense;
import com.ajapplications.budgeteerbuddy.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * The adapter for the {@link MonthlyReportFragment} recycler view.
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    /**
     * Type of cell for an {@link Expense}
     */
    private static final int EXPENSE_VIEW_TYPE = 1;
    /**
     * Type of cell for a header
     */
    private static final int HEADER_VIEW_TYPE = 2;

    /**
     * Formatter to get day number for each date
     */
    private static final SimpleDateFormat dayFormatter = new SimpleDateFormat("dd", Locale.getDefault());

    /**
     * List of expenses (may be empty)
     */
    @NonNull
    private final List<Expense> expenses;
    /**
     * List of revenues (may be empty)
     */
    @NonNull
    private final List<Expense> revenues;

// --------------------------------------->

    /**
     *
     * @param expenses
     * @param revenues
     */
    public MonthlyReportRecyclerViewAdapter(@NonNull List<Expense> expenses, @NonNull List<Expense> revenues)
    {
        this.expenses = expenses;
        this.revenues = revenues;
    }

// --------------------------------------->

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if( HEADER_VIEW_TYPE == viewType )
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_monthly_report_header_cell, parent, false);
            return new HeaderViewHolder(v);
        }

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_monthly_report_expense_cell, parent, false);
        return new ExpenseViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        if( holder instanceof HeaderViewHolder )
        {
            boolean isRevenuesHeader = isRevenuesHeader(position);

            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerTitle.setText(isRevenuesHeader ? R.string.revenues : R.string.expenses);
            headerHolder.view.setBackgroundColor(ContextCompat.getColor(headerHolder.view.getContext(), isRevenuesHeader ? R.color.budget_green : R.color.budget_red));
        }
        else
        {
            ExpenseViewHolder viewHolder = (ExpenseViewHolder) holder;
            Expense expense = getExpense(position);

            viewHolder.expenseTitleTextView.setText(expense.getTitle());
            viewHolder.expenseAmountTextView.setText(CurrencyHelper.getFormattedCurrencyString(viewHolder.view.getContext(), -expense.getAmount()));
            viewHolder.expenseAmountTextView.setTextColor(ContextCompat.getColor(viewHolder.view.getContext(), expense.isRevenue() ? R.color.budget_green : R.color.budget_red));
            viewHolder.monthlyIndicator.setVisibility(expense.isRecurring() ? View.VISIBLE : View.GONE);
            viewHolder.dateTextView.setText(dayFormatter.format(expense.getDate()));
        }
    }

    @Override
    public int getItemCount()
    {
        return (expenses.isEmpty() ? 0 : expenses.size() + 1) + (revenues.isEmpty() ? 0 : revenues.size() + 1);
    }

    @Override
    public int getItemViewType(int position)
    {
        return isHeader(position) ? HEADER_VIEW_TYPE : EXPENSE_VIEW_TYPE;
    }

    /**
     * Get the expense for the given position
     *
     * @param position the position
     * @return the expense for that position
     */
    private Expense getExpense(int position)
    {
        if( !revenues.isEmpty() && position - 1 < revenues.size() )
        {
            return revenues.get(position - 1);
        }

        int expensesHeaderDelta = 1 + (revenues.isEmpty() ? 0 : 1);
        return expenses.get(position - expensesHeaderDelta - revenues.size());
    }

    /**
     * Is the given position an header cell
     *
     * @param position the position
     * @return true if it's an header, false otherwise
     */
    private boolean isHeader(int position)
    {
        return isExpensesHeader(position) || isRevenuesHeader(position);
    }

    /**
     * Is the given position the expense header cell
     *
     * @param position the position
     * @return true if it's the expense header, false otherwise
     */
    private boolean isExpensesHeader(int position)
    {
        return !expenses.isEmpty() && position == revenues.size() + (revenues.isEmpty() ? 0 : 1);
    }

    /**
     * Is the given position the revenue header cell
     *
     * @param position the position
     * @return true if it's the revenue header, false otherwise
     */
    private boolean isRevenuesHeader(int position)
    {
        return !revenues.isEmpty() && position == 0;
    }

// --------------------------------------->

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder
    {
        public final TextView expenseTitleTextView;
        public final TextView expenseAmountTextView;
        public final ViewGroup monthlyIndicator;
        public final TextView dateTextView;
        public final View view;

        public ExpenseViewHolder(View v)
        {
            super(v);

            view = v;
            expenseTitleTextView = (TextView) v.findViewById(R.id.expense_title);
            expenseAmountTextView = (TextView) v.findViewById(R.id.expense_amount);
            monthlyIndicator = (ViewGroup) v.findViewById(R.id.recurring_indicator);
            dateTextView = (TextView) v.findViewById(R.id.date_tv);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder
    {
        public final TextView headerTitle;
        public final View view;

        public HeaderViewHolder(View v)
        {
            super(v);

            view = v;
            headerTitle = (TextView) v.findViewById(R.id.monthly_recycler_view_header_tv);
        }
    }
}
