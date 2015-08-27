package com.benoitletondor.easybudget.view;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.helper.CurrencyHelper;
import com.benoitletondor.easybudget.helper.ParameterKeys;
import com.benoitletondor.easybudget.helper.Parameters;

import java.util.Currency;
import java.util.Locale;

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
public class PreferencesFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from the XML resource
        addPreferencesFromResource(R.xml.preferences);

        /*
         * Bind bug report button
         */
        findPreference(getResources().getString(R.string.setting_category_bug_report_send_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                String localId = Parameters.getInstance(getActivity()).getString(ParameterKeys.LOCAL_ID);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.setting_category_bug_report_send_text) + localId);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                return false;
            }
        });

        /*
         * Currency change button
         */
        Preference currencyPreference = findPreference(getResources().getString(R.string.setting_category_currency_change_button_key));
        currencyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {


                return false;
            }
        });
        currencyPreference.setTitle(String.format(Locale.US, getResources().getString(R.string.setting_category_currency_change_button_title), CurrencyHelper.getUserCurrency(getActivity()).getSymbol()));
    }


}
