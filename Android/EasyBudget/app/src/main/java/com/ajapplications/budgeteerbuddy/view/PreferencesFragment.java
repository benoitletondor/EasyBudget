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

package com.ajapplications.budgeteerbuddy.view;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.ajapplications.budgeteerbuddy.EasyBudget;
import com.ajapplications.budgeteerbuddy.PremiumCheckStatus;
import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.helper.Logger;
import com.ajapplications.budgeteerbuddy.helper.ParameterKeys;
import com.ajapplications.budgeteerbuddy.helper.Parameters;
import com.ajapplications.budgeteerbuddy.helper.UIHelper;
import com.ajapplications.budgeteerbuddy.helper.UserHelper;
import com.ajapplications.budgeteerbuddy.notif.DailyNotifOptinService;
import com.ajapplications.budgeteerbuddy.notif.MonthlyReportNotifService;
import com.ajapplications.budgeteerbuddy.view.selectcurrency.SelectCurrencyFragment;
import com.ajapplications.budgeteerbuddy.BuildConfig;
import com.ajapplications.budgeteerbuddy.R;
import com.google.android.gms.appinvite.AppInviteInvitation;

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

    /**
     * Category containing premium features (shown to premium users)
     */
    private PreferenceCategory premiumCategory;
    /**
     * Category containing ways to become premium (shown to not premium users)
     */
    private PreferenceCategory notPremiumCategory;
    /**
     * Is the premium category shown
     */
    private boolean premiumShown = true;
    /**
     * Is the not premium category shown
     */
    private boolean notPremiumShown = true;

// ---------------------------------------->

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from the XML resource
        addPreferencesFromResource(R.xml.preferences);

        /*
         * Rating button
         */
        findPreference(getResources().getString(R.string.setting_category_rate_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                new RatingPopup(getActivity()).show(true);
                return false;
            }
        });

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
         * Share app
         */
        findPreference(getResources().getString(R.string.setting_category_share_app_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                try
                {
                    Intent intent = new AppInviteInvitation.IntentBuilder(getResources().getString(R.string.app_invite_title))
                            .setMessage(getResources().getString(R.string.app_invite_message))
                            .setDeepLink(Uri.parse(MainActivity.buildAppInvitesReferrerDeeplink(getActivity())))
                            .build();

                    ActivityCompat.startActivityForResult(getActivity(), intent, SettingsActivity.APP_INVITE_REQUEST, null);
                }
                catch (Exception e)
                {
                    Logger.error("An error occured during app invites activity start", e);
                }

                return false;
            }
        });

        /*
         * App version
         */
        final Preference appVersionPreference = findPreference(getResources().getString(R.string.setting_category_app_version_key));
        appVersionPreference.setTitle(getResources().getString(R.string.setting_category_app_version_title, BuildConfig.VERSION_NAME));
        appVersionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://twitter.com/BenoitLetondor"));
                getActivity().startActivity(i);

                return false;
            }
        });

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
         * Warning limit button
         */
        final Preference limitWarningPreference = findPreference(getResources().getString(R.string.setting_category_limit_set_button_key));
        limitWarningPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_warning_limit, null);
                final EditText limitEditText = (EditText) dialogView.findViewById(R.id.warning_limit);
                limitEditText.setText(String.valueOf(Parameters.getInstance(getActivity()).getInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, EasyBudget.DEFAULT_LOW_MONEY_WARNING_AMOUNT)));
                limitEditText.setSelection(limitEditText.getText().length()); // Put focus at the end of the text

                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.adjust_limit_warning_title);
                builder.setMessage(R.string.adjust_limit_warning_message);
                builder.setView(dialogView);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(final DialogInterface dialog, int which)
                    {
                        String limitString = limitEditText.getText().toString();
                        if (limitString.trim().isEmpty())
                        {
                            limitString = "0"; // Set a 0 value if no value is provided (will lead to an error displayed to the user)
                        }

                        try
                        {
                            int newLimit = Integer.valueOf(limitString);

                            // Invalid value, alert the user
                            if (newLimit <= 0)
                            {
                                throw new IllegalArgumentException("limit should be > 0");
                            }

                            Parameters.getInstance(getActivity()).putInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, newLimit);
                            setLimitWarningPreferenceTitle(limitWarningPreference);
                        }
                        catch (Exception e)
                        {
                            new AlertDialog.Builder(getActivity()).setTitle(R.string.adjust_limit_warning_error_title).setMessage(getResources().getString(R.string.adjust_limit_warning_error_message)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }

                            }).show();
                        }
                    }
                });

                final Dialog dialog = builder.show();

                // Directly show keyboard when the dialog pops
                limitEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
                {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus)
                    {
                        if (hasFocus && getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) // Check if the device doesn't have a physical keyboard
                        {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                });

                return false;
            }
        });
        setLimitWarningPreferenceTitle(limitWarningPreference);

        /*
         * Premium status
         */
        premiumCategory = (PreferenceCategory) findPreference(getResources().getString(R.string.setting_category_premium_key));
        notPremiumCategory = (PreferenceCategory) findPreference(getResources().getString(R.string.setting_category_not_premium_key));
        refreshPremiumPreference();

        /*
         * Notifications
         */
        final CheckBoxPreference updateNotifPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.setting_category_notifications_update_key));
        updateNotifPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                UserHelper.setUserAllowUpdatePushes(getActivity(), updateNotifPref.isChecked());
                return true;
            }
        });
        updateNotifPref.setChecked(UserHelper.isUserAllowingUpdatePushes(getActivity()));

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

            /*
             * Show premium screen
             */
            findPreference(getResources().getString(R.string.setting_category_dev_show_premium_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    showBecomePremiumDialog();
                    return false;
                }
            });

            /*
             * Show daily reminder opt-in notif
             */
            findPreference(getResources().getString(R.string.setting_category_show_notif_daily_reminder_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    DailyNotifOptinService.showDailyReminderOptinNotif(getActivity());
                    return false;
                }
            });

            /*
             * Show monthly report notif for premium users
             */
            findPreference(getResources().getString(R.string.setting_category_show_notif_monthly_premium_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    MonthlyReportNotifService.showPremiumNotif(getActivity());
                    return false;
                }
            });

            /*
             * Show monthly report notif for non premium users
             */
            findPreference(getResources().getString(R.string.setting_category_show_notif_monthly_notpremium_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    MonthlyReportNotifService.showNotPremiumNotif(getActivity());
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
        }

        /*
         * Broadcast receiver
         */
        IntentFilter filter = new IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
        filter.addAction(EasyBudget.INTENT_IAB_STATUS_CHANGED);
        filter.addAction(SettingsActivity.USER_GONE_PREMIUM_INTENT);
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( SelectCurrencyFragment.CURRENCY_SELECTED_INTENT.equals(intent.getAction()) && selectCurrencyDialog != null )
                {
                    setCurrencyPreferenceTitle(currencyPreference);

                    selectCurrencyDialog.dismiss();
                    selectCurrencyDialog = null;
                }
                else if( EasyBudget.INTENT_IAB_STATUS_CHANGED.equals(intent.getAction()) )
                {
                    try
                    {
                        PremiumCheckStatus status = (PremiumCheckStatus) intent.getSerializableExtra(EasyBudget.INTENT_IAB_STATUS_KEY);
                        if( status == PremiumCheckStatus.PREMIUM )
                        {
                            refreshPremiumPreference();
                        }
                    }
                    catch (Exception e)
                    {
                        Logger.error("Error while receiving INTENT_IAB_STATUS_CHANGED intent", e);
                    }
                }
                else if( SettingsActivity.USER_GONE_PREMIUM_INTENT.equals(intent.getAction()) )
                {
                    new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.iab_purchase_success_title)
                        .setMessage(R.string.iab_purchase_success_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        })
                        .show();

                    refreshPremiumPreference();
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        /*
         * Check if we should show premium popup
         */
        if( getActivity().getIntent().getBooleanExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, false) )
        {
            showBecomePremiumDialog();
        }
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

    /**
     * Set the limit warning preference title according to the selected limit
     *
     * @param limitWarningPreferenceTitle
     */
    private void setLimitWarningPreferenceTitle(Preference limitWarningPreferenceTitle)
    {
        limitWarningPreferenceTitle.setTitle(getResources().getString(R.string.setting_category_limit_set_button_title, CurrencyHelper.getFormattedCurrencyString(getActivity(), Parameters.getInstance(getActivity()).getInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, EasyBudget.DEFAULT_LOW_MONEY_WARNING_AMOUNT))));
    }

    /**
     * Show the right premium preference depending on the user state
     */
    private void refreshPremiumPreference()
    {
        boolean isPremium = UserHelper.isUserPremium(getActivity().getApplication());

        if( isPremium )
        {
            if( notPremiumShown )
            {
                getPreferenceScreen().removePreference(notPremiumCategory);
                notPremiumShown = false;
            }

            if( !premiumShown )
            {
                getPreferenceScreen().addPreference(premiumCategory);
                premiumShown = true;
            }

            // Premium preference
            findPreference(getResources().getString(R.string.setting_category_premium_status_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.premium_popup_premium_title)
                            .setMessage(R.string.premium_popup_premium_message)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            })
                            .show();

                    return false;
                }
            });

            // Daily reminder notif preference
            final CheckBoxPreference dailyNotifPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.setting_category_notifications_daily_key));
            dailyNotifPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    UserHelper.setUserAllowDailyReminderPushes(getActivity(), dailyNotifPref.isChecked());
                    return true;
                }
            });
            dailyNotifPref.setChecked(UserHelper.isUserAllowingDailyReminderPushes(getActivity()));

            // Monthly reminder for reports
            final CheckBoxPreference monthlyNotifPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.setting_category_notifications_monthly_key));
            monthlyNotifPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    UserHelper.setUserAllowMonthlyReminderPushes(getActivity(), monthlyNotifPref.isChecked());
                    return true;
                }
            });
            monthlyNotifPref.setChecked(UserHelper.isUserAllowingMonthlyReminderPushes(getActivity()));
        }
        else
        {
            if( premiumShown )
            {
                getPreferenceScreen().removePreference(premiumCategory);
                premiumShown = false;
            }

            if( !notPremiumShown )
            {
                getPreferenceScreen().addPreference(notPremiumCategory);
                notPremiumShown = true;
            }

            // Not premium preference
            findPreference(getResources().getString(R.string.setting_category_not_premium_status_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    showBecomePremiumDialog();
                    return false;
                }
            });

            // Redeem promo code pref
            findPreference(getResources().getString(R.string.setting_category_premium_redeem_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_redeem_voucher, null);
                    final EditText voucherEditText = (EditText) dialogView.findViewById(R.id.voucher);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.voucher_redeem_dialog_title)
                        .setMessage(R.string.voucher_redeem_dialog_message)
                        .setView(dialogView)
                        .setPositiveButton(R.string.voucher_redeem_dialog_cta, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();

                                String promocode = voucherEditText.getText().toString();
                                if( promocode.trim().isEmpty() )
                                {
                                    new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.voucher_redeem_error_dialog_title)
                                        .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();

                                    return;
                                }

                                if( !((EasyBudget) getActivity().getApplication()).launchRedeemPromocodeFlow(promocode, getActivity()) )
                                {
                                    new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.iab_purchase_error_title)
                                        .setMessage(getResources().getString(R.string.iab_purchase_error_message, "Error redeeming promo code"))
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });

                    final Dialog dialog = builder.show();

                    // Directly show keyboard when the dialog pops
                    voucherEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
                    {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus)
                        {
                            if (hasFocus && getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS ) // Check if the device doesn't have a physical keyboard
                            {
                                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            }
                        }
                    });

                    return false;
                }
            });
        }
    }

    private void showBecomePremiumDialog()
    {
        Intent intent = new Intent(getActivity(), PremiumActivity.class);
        ActivityCompat.startActivityForResult(getActivity(), intent, SettingsActivity.PREMIUM_ACTIVITY, null);
    }

    @Override
    public void onDestroy()
    {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);

        super.onDestroy();
    }
}
