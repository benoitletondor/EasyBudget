package com.benoitletondor.easybudget.view.selectcurrency;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
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
public class SelectCurrencyFragment extends DialogFragment
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
        if( getShowsDialog() )
        {
            return null;
        }

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_select_currency, container, false);

        setupRecyclerView(v);

        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_select_currency, null, false);
        setupRecyclerView(v);

        // Put some padding between title and content
        v.setPadding(0, getActivity().getResources().getDimensionPixelSize(R.dimen.select_currency_dialog_padding_top), 0, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(v);
        builder.setTitle(R.string.setting_category_currency_change_dialog_title);

        return builder.create();
    }

// ------------------------------------->

    /**
     * Setup the recycler view
     *
     * @param v inflated view
     */
    private void setupRecyclerView(View v)
    {
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.select_currency_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        SelectCurrencyRecyclerViewAdapter adapter = new SelectCurrencyRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll to currently selected currency
        recyclerView.scrollToPosition(adapter.getSelectedCurrencyPosition(v.getContext()));
    }
}
