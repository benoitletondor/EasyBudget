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

package com.benoitletondor.easybudgetapp.view.main;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType;
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Recycler view adapter to display expenses for a given date
 *
 * @author Benoit LETONDOR
 */
public class ExpensesRecyclerViewAdapter extends RecyclerView.Adapter<ExpensesRecyclerViewAdapter.ViewHolder>
{
    @NonNull
    private List<Expense> expenses = new ArrayList<>(0);
    @NonNull
    private Date date;
    @NonNull
    private final Activity activity;
    @NonNull
    private final Parameters parameters;

// ------------------------------------------->

    /**
     * Instanciate an adapter for the given date
     */
    public ExpensesRecyclerViewAdapter(@NonNull Activity activity, @NonNull Parameters parameters, @NonNull Date date)
    {
        this.activity = activity;
        this.parameters = parameters;
        this.date = date;
    }

    /**
     * Return the date content is displayed for displayed
     */
    @NonNull
    public Date getDate()
    {
        return date;
    }

    /**
     * Set a new date and data to display
     */
    public void setDate(@NonNull Date date, @NonNull List<Expense> expenses)
    {
        this.date = date;
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    /**
     * Remove given expense
     *
     * @param expense the expense to remove
     * @return position of the deleted expense (-1 if not found)
     */
    public int removeExpense(Expense expense)
    {
        Iterator<Expense> expenseIterator = expenses.iterator();
        int position = 0;
        while( expenseIterator.hasNext() )
        {
            Expense shownExpense = expenseIterator.next();
            if( shownExpense.getId().equals(expense.getId()) )
            {
                expenseIterator.remove();
                notifyItemRemoved(position);
                return position;
            }

            position++;
        }

        return -1;
    }

    /**
     * Add an expense at the given position
     */
    public void addExpense(Expense expense, int position)
    {
        expenses.add(position, expense);
        notifyItemRangeInserted(position, 1);
    }

// ------------------------------------------>

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycleview_expense_cell, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i)
    {
        final Expense expense = expenses.get(i);

        viewHolder.expenseTitleTextView.setText(expense.getTitle());
        viewHolder.expenseAmountTextView.setText(CurrencyHelper.getFormattedCurrencyString(parameters, -expense.getAmount()));
        viewHolder.expenseAmountTextView.setTextColor(ContextCompat.getColor(viewHolder.view.getContext(), expense.isRevenue() ? R.color.budget_green : R.color.budget_red));
        viewHolder.recurringIndicator.setVisibility(expense.isRecurring() ? View.VISIBLE : View.GONE);
        viewHolder.positiveIndicator.setImageResource(expense.isRevenue() ? R.drawable.ic_label_green : R.drawable.ic_label_red);

        if( expense.isRecurring() )
        {
            assert expense.getAssociatedRecurringExpense() != null;
            switch (expense.getAssociatedRecurringExpense().getType())
            {
                case WEEKLY:
                    viewHolder.recurringIndicatorTextview.setText(viewHolder.view.getContext().getString(R.string.weekly));
                    break;
                case BI_WEEKLY:
                    viewHolder.recurringIndicatorTextview.setText(viewHolder.view.getContext().getString(R.string.bi_weekly));
                    break;
                case MONTHLY:
                    viewHolder.recurringIndicatorTextview.setText(viewHolder.view.getContext().getString(R.string.monthly));
                    break;
                case YEARLY:
                    viewHolder.recurringIndicatorTextview.setText(viewHolder.view.getContext().getString(R.string.yearly));
                    break;
            }
        }

        final View.OnClickListener onClickListener = v -> {
            if (expense.isRecurring())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(expense.isRevenue() ? R.string.dialog_edit_recurring_income_title : R.string.dialog_edit_recurring_expense_title);
                builder.setItems(expense.isRevenue() ? R.array.dialog_edit_recurring_income_choices : R.array.dialog_edit_recurring_expense_choices, (dialog, which) -> {
                    switch ( which )
                    {
                        case 0: // Edit this one
                        {
                            Intent startIntent = new Intent(viewHolder.view.getContext(), ExpenseEditActivity.class);
                            startIntent.putExtra("date", expense.getDate().getTime());
                            startIntent.putExtra("expense", expense);

                            ActivityCompat.startActivityForResult(activity, startIntent, MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null);

                            break;
                        }
                        case 1: // Delete this one
                        {
                            // Send notification to inform views that this expense has been deleted
                            Intent intent = new Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED);
                            intent.putExtra("expense", expense);
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.ONE.getValue());
                            LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

                            break;
                        }
                        case 2: // Delete from
                        {
                            // Send notification to inform views that this expense has been deleted
                            Intent intent = new Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED);
                            intent.putExtra("expense", expense);
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.FROM.getValue());
                            LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

                            break;
                        }
                        case 3: // Delete up to
                        {
                            // Send notification to inform views that this expense has been deleted
                            Intent intent = new Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED);
                            intent.putExtra("expense", expense);
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.TO.getValue());
                            LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

                            break;
                        }
                        case 4: // Delete all
                        {
                            // Send notification to inform views that this expense has been deleted
                            Intent intent = new Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED);
                            intent.putExtra("expense", expense);
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.ALL.getValue());
                            LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

                            break;
                        }
                    }
                });
                builder.show();
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(expense.isRevenue() ? R.string.dialog_edit_income_title : R.string.dialog_edit_expense_title);
                builder.setItems(expense.isRevenue() ? R.array.dialog_edit_income_choices : R.array.dialog_edit_expense_choices, (dialog, which) -> {
                    switch ( which )
                    {
                        case 0: // Edit expense
                        {
                            Intent startIntent = new Intent(viewHolder.view.getContext(), ExpenseEditActivity.class);
                            startIntent.putExtra("date", expense.getDate().getTime());
                            startIntent.putExtra("expense", expense);

                            ActivityCompat.startActivityForResult(activity, startIntent, MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null);

                            break;
                        }
                        case 1: // Delete
                        {
                            // Send notification to inform views that this expense has been deleted
                            Intent intent = new Intent(MainActivity.INTENT_EXPENSE_DELETED);
                            intent.putExtra("expense", expense);
                            LocalBroadcastManager.getInstance(activity.getApplicationContext()).sendBroadcast(intent);

                            break;
                        }
                    }
                });
                builder.show();
            }

        };

        viewHolder.view.setOnClickListener(onClickListener);

        viewHolder.view.setOnLongClickListener(v -> {
            onClickListener.onClick(v);
            return true;
        });
    }

    @Override
    public int getItemCount()
    {
        return expenses.size();
    }

// ------------------------------------------->

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView expenseTitleTextView;
        final TextView expenseAmountTextView;
        final ViewGroup recurringIndicator;
        final TextView recurringIndicatorTextview;
        final ImageView positiveIndicator;
        final View view;

        ViewHolder(View v)
        {
            super(v);

            view = v;
            expenseTitleTextView = v.findViewById(R.id.expense_title);
            expenseAmountTextView = v.findViewById(R.id.expense_amount);
            recurringIndicator = v.findViewById(R.id.recurring_indicator);
            recurringIndicatorTextview = v.findViewById(R.id.recurring_indicator_textview);
            positiveIndicator = v.findViewById(R.id.positive_indicator);
        }
    }
}