package com.benoitletondor.easybudget.view.expenses;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by benoit on 25/12/14.
 */
public class ExpensesRecyclerViewAdapter extends RecyclerView.Adapter<ExpensesRecyclerViewAdapter.ViewHolder>
{
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i)
    {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i)
    {

    }

    @Override
    public int getItemCount()
    {
        return 0;
    }

// ------------------------------------------->

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        // each data item is just a string in this case
        public TextView mTextView;

        public ViewHolder(TextView v)
        {
            super(v);
            mTextView = v;
        }
    }
}
