package com.benoitletondor.easybudget.view.expenses;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.model.Expense;
import com.benoitletondor.easybudget.model.OneTimeExpense;
import com.benoitletondor.easybudget.model.db.DB;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Benoit LETONDOR
 */
public class ExpensesRecyclerViewAdapter extends RecyclerView.Adapter<ExpensesRecyclerViewAdapter.ViewHolder>
{
    private List<Expense> expenses = new ArrayList<>();

    public ExpensesRecyclerViewAdapter(DB db, Date date)
    {
        if (db == null)
        {
            throw new NullPointerException("db==null");
        }

        if (date == null)
        {
            throw new NullPointerException("date==null");
        }

        List<OneTimeExpense> expenses = db.getOneTimeExpensesForDay(date);
        this.expenses.addAll(expenses);
    }

// ------------------------------------------>

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycleview_expense_cell, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i)
    {
        viewHolder.textView.setText("Amount : "+expenses.get(i).getAmount());
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
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        // each data item is just a string in this case
        public TextView textView;

        public ViewHolder(View v)
        {
            super(v);

            textView = (TextView) v.findViewById(R.id.expense_text_view);
        }
    }
}
