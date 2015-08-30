package com.benoitletondor.easybudget.view.selectcurrency;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.benoitletondor.easybudget.R;

import java.util.Currency;

/**
 * Fragment that contains UI for user to chose its currency.<br />
 * You should listen to the {@link #CURRENCY_SELECTED_INTENT} intent to get notified when
 * the selected currency has changed. The newly selected currency ISO code is available in
 * the {@link #CURRENCY_ISO_EXTRA} string extra.<br />
 * <br />
 * NB: The {@link com.benoitletondor.easybudget.helper.CurrencyHelper#setUserCurrency(Context, Currency)}
 * method is automaticaly called by the fragment on selection, you don't have to do it yourself.
 *
 * @author Benoit LETONDOR
 */
public class SelectCurrencyFragment extends Fragment
{
    /**
     * Action of the intent broadcasted when selected currency has changed
     */
    public static final String CURRENCY_SELECTED_INTENT = "currency.selected";
    /**
     * Key to retrieve the newly selected currency ISO code in Intent extras
     */
    public final static String CURRENCY_ISO_EXTRA       = "currency.iso.key";

// -------------------------------------->

    public SelectCurrencyFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_select_currency, container, false);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.select_currency_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        SelectCurrencyRecyclerViewAdapter adapter = new SelectCurrencyRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll to currently selected currency
        recyclerView.scrollToPosition(adapter.getSelectedCurrencyPosition(v.getContext()));

        return v;
    }


}
