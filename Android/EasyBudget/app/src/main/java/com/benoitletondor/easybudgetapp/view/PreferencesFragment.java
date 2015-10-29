package com.benoitletondor.easybudgetapp.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.benoitletondor.easybudgetapp.BuildConfig;
import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment;

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
public class PreferencesFragment extends PreferenceFragment
{
    /**
     * The dialog to select a new currency (will be null if not shown)
     */
    private SelectCurrencyFragment selectCurrencyDialog;
    /**
     * Broadcast receiver (used for currency selection)
     */
    private BroadcastReceiver      receiver;

// ---------------------------------------->

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
                sendIntent.setAction(Intent.ACTION_SENDTO);
                sendIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.bug_report_email)});
                sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.setting_category_bug_report_send_text, localId));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.setting_category_bug_report_send_subject));

                if (sendIntent.resolveActivity(getActivity().getPackageManager()) != null)
                {
                    startActivity(sendIntent);
                }
                else
                {
                    Toast.makeText(getActivity(), getResources().getString(R.string.setting_category_bug_report_send_error), Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });

        /*
         * Enable animations pref
         */
        final CheckBoxPreference animationsPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.setting_category_disable_animation_key));
        animationsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                UIHelper.setAnimationsEnabled(getActivity(), animationsPref.isChecked());
                return true;
            }
        });
        animationsPref.setChecked(UIHelper.areAnimationsEnabled(getActivity()));

        /*
         * Currency change button
         */
        final Preference currencyPreference = findPreference(getResources().getString(R.string.setting_category_currency_change_button_key));
        currencyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                selectCurrencyDialog = new SelectCurrencyFragment();
                selectCurrencyDialog.show(((SettingsActivity) getActivity()).getSupportFragmentManager(), "SelectCurrency");

                return false;
            }
        });
        setCurrencyPreferenceTitle(currencyPreference);

        /*
         * Hide dev preferences if needed
         */
        PreferenceCategory devCategory = (PreferenceCategory) findPreference(getResources().getString(R.string.setting_category_dev_key));
        if( !BuildConfig.DEV_PREFERENCES )
        {
            getPreferenceScreen().removePreference(devCategory);
        }
        else
        {
            /*
             * Show welcome screen button
             */
            findPreference(getResources().getString(R.string.setting_category_show_welcome_screen_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(MainActivity.INTENT_SHOW_WELCOME_SCREEN));

                    getActivity().finish();
                    return false;
                }
            });
        }


        /*
         * Broadcast receiver
         */
        IntentFilter filter = new IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( selectCurrencyDialog != null )
                {
                    setCurrencyPreferenceTitle(currencyPreference);

                    selectCurrencyDialog.dismiss();
                    selectCurrencyDialog = null;
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    /**
     * Set the currency preference title according to selected currency
     *
     * @param currencyPreference
     */
    private void setCurrencyPreferenceTitle(Preference currencyPreference)
    {
        currencyPreference.setTitle(getResources().getString(R.string.setting_category_currency_change_button_title, CurrencyHelper.getUserCurrency(getActivity()).getSymbol()));
    }

    @Override
    public void onDestroy()
    {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);

        super.onDestroy();
    }
}
