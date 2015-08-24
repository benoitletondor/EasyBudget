package com.benoitletondor.easybudget.view.monthly;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.model.MonthlyExpense;
import com.benoitletondor.easybudget.model.db.DB;

import java.util.List;

/**
 * @author Benoit LETONDOR
 */
public class MonthlyRecyclerViewAdapter extends RecyclerView.Adapter<MonthlyRecyclerViewAdapter.ViewHolder>
{
    private List<MonthlyExpense> expenses;

// ------------------------------------------>

    public MonthlyRecyclerViewAdapter(DB db)
    {
        if (db == null)
        {
            throw new NullPointerException("db==null");
        }

        this.expenses = db.getAllMonthlyExpenses();
    }

// ------------------------------------------>

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleview_montly_expense_cell, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        final MonthlyExpense expense = expenses.get(position);

        viewHolder.expenseTitleTextView.setText(expense.getTitle());
        viewHolder.expenseAmountTextView.setText(-expense.getAmount() + " €");
        viewHolder.positiveIndicator.setImageResource(expense.getAmount() < 0 ? R.drawable.ic_label_green : R.drawable.ic_label_red);
    }

    @Override
    public int getItemCount()
    {
        return expenses.size();
    }

// ------------------------------------------->

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final TextView  expenseTitleTextView;
        public final TextView  expenseAmountTextView;
        public final ImageView positiveIndicator;
        public final View      view;

        public ViewHolder(View v)
        {
            super(v);

            view = v;
            expenseTitleTextView = (TextView) v.findViewById(R.id.expense_title);
            expenseAmountTextView = (TextView) v.findViewById(R.id.expense_amount);
            positiveIndicator = (ImageView) v.findViewById(R.id.positive_indicator);
        }
    }
}
