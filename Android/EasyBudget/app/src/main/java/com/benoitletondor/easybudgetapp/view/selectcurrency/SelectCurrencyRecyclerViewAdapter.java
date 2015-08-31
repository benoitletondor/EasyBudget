package com.benoitletondor.easybudgetapp.view.selectcurrency;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;

import java.util.Currency;
import java.util.List;

/**
 * View adapter for the Recycler view of the {@link SelectCurrencyFragment}
 *
 * @author Benoit LETONDOR
 */
public class SelectCurrencyRecyclerViewAdapter extends RecyclerView.Adapter<SelectCurrencyRecyclerViewAdapter.ViewHolder>
{
    /**
     * List of available currencies
     */
    private final List<Currency> currencies = CurrencyHelper.getAvailableCurrencies();

// ---------------------------------------->

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleview_currency_cell, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        final Currency currency = currencies.get(position);

        boolean userCurrency = CurrencyHelper.getUserCurrency(holder.view.getContext()).equals(currency);

        holder.selectedIndicator.setVisibility(userCurrency ? View.VISIBLE : View.INVISIBLE);
        holder.currencyTitle.setText(CurrencyHelper.getCurrencyDisplayName(currency));
        holder.view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Set the currency
                CurrencyHelper.setUserCurrency(v.getContext(), currency);
                // Reload date to change the checkmark
                notifyDataSetChanged();

                // Broadcast the intent
                Intent intent = new Intent(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
                intent.putExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA, currency.getCurrencyCode());

                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return currencies.size();
    }

// ------------------------------------------->

    /**
     * Get the position of the selected currency
     *
     * @param context
     * @return
     */
    public int getSelectedCurrencyPosition(@NonNull Context context)
    {
        return currencies.indexOf(CurrencyHelper.getUserCurrency(context));
    }

// ------------------------------------------->

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public View view;
        public TextView currencyTitle;
        public ImageView selectedIndicator;

        public ViewHolder(View v)
        {
            super(v);

            view = v;
            currencyTitle = (TextView) v.findViewById(R.id.currency_cell_title_tv);
            selectedIndicator = (ImageView) v.findViewById(R.id.currency_cell_selected_indicator_iv);
        }
    }
}
