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

package com.benoitletondor.easybudgetapp.view.selectcurrency;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.Parameters;

import java.util.Currency;
import java.util.List;

/**
 * View adapter for the Recycler view of the {@link SelectCurrencyFragment}
 *
 * @author Benoit LETONDOR
 */
public class SelectCurrencyRecyclerViewAdapter extends RecyclerView.Adapter<SelectCurrencyRecyclerViewAdapter.ViewHolder>
{
    private final static int TYPE_MAIN_CURRENCY = 0;
    private final static int TYPE_SEPARATOR = 1;
    private final static int TYPE_SECONDARY_CURRENCY = 2;

    /**
     * List of main currencies
     */
    private final List<Currency> mainCurrencies;
    /**
     * List of secondary currencies
     */
    private final List<Currency> secondaryCurrencies;
    @NonNull
    private final Parameters parameters;

// ---------------------------------------->

    public SelectCurrencyRecyclerViewAdapter(@NonNull List<Currency> mainCurrencies, @NonNull List<Currency> secondaryCurrencies, @NonNull Parameters parameters)
    {
        this.mainCurrencies = mainCurrencies;
        this.secondaryCurrencies = secondaryCurrencies;
        this.parameters = parameters;
    }

// ---------------------------------------->

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if( viewType == TYPE_MAIN_CURRENCY || viewType == TYPE_SECONDARY_CURRENCY )
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleview_currency_cell, parent, false);
            return new ViewHolder(v, viewType);
        }
        else
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycleview_currency_separator_cell, parent, false);
            return new ViewHolder(v, viewType, true);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        if( !holder.separator )
        {
            final Currency currency = holder.type == TYPE_MAIN_CURRENCY ? mainCurrencies.get(position) : secondaryCurrencies.get(position - 1 - mainCurrencies.size());

            boolean userCurrency = CurrencyHelper.getUserCurrency(parameters).equals(currency);

            holder.selectedIndicator.setVisibility(userCurrency ? View.VISIBLE : View.INVISIBLE);
            holder.currencyTitle.setText(CurrencyHelper.getCurrencyDisplayName(currency));
            holder.view.setOnClickListener(v -> {
                // Set the currency
                CurrencyHelper.setUserCurrency(parameters, currency);
                // Reload date to change the checkmark
                notifyDataSetChanged();

                // Broadcast the intent
                Intent intent = new Intent(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
                intent.putExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA, currency.getCurrencyCode());

                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
            });
        }
    }

    @Override
    public int getItemCount()
    {
        return mainCurrencies.size() + 1 +secondaryCurrencies.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        if( position < mainCurrencies.size() )
        {
            return TYPE_MAIN_CURRENCY;
        }
        else if( position == mainCurrencies.size() )
        {
            return TYPE_SEPARATOR;
        }
        else
        {
            return TYPE_SECONDARY_CURRENCY;
        }
    }
// ------------------------------------------->

    /**
     * Get the position of the selected currency
     */
    public int getSelectedCurrencyPosition()
    {
        Currency currency = CurrencyHelper.getUserCurrency(parameters);

        return mainCurrencies.contains(currency) ? mainCurrencies.indexOf(currency) : (secondaryCurrencies.indexOf(currency) + 1 + mainCurrencies.size() );
    }

// ------------------------------------------->

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final boolean separator;
        public final int type;

        public View view;
        public TextView currencyTitle;
        public ImageView selectedIndicator;

        public ViewHolder(View v, int type, boolean separator)
        {
            super(v);

            this.separator = separator;
            this.type = type;

            if( !separator )
            {
                view = v;
                currencyTitle = v.findViewById(R.id.currency_cell_title_tv);
                selectedIndicator = v.findViewById(R.id.currency_cell_selected_indicator_iv);
            }
        }

        public ViewHolder(View v, int type)
        {
            this(v, type, false);
        }
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder
    {

        public SeparatorViewHolder(View itemView)
        {
            super(itemView);
        }
    }
}
