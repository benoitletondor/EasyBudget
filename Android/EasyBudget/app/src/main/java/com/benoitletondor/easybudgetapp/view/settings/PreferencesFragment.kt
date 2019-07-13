/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.*
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.INTENT_IAB_STATUS_CHANGED
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.notif.DarkThemeNotif
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.USER_GONE_PREMIUM_INTENT
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.premium.PremiumActivity
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.SHOW_THEME_INTENT_KEY
import com.roomorama.caldroid.CaldroidFragment
import org.koin.android.ext.android.inject
import java.net.URLEncoder

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
class PreferencesFragment : PreferenceFragmentCompat() {

    /**
     * The dialog to select a new currency (will be null if not shown)
     */
    private var selectCurrencyDialog: SelectCurrencyFragment? = null
    /**
     * Broadcast receiver (used for currency selection)
     */
    private lateinit var receiver: BroadcastReceiver

    /**
     * Category containing premium features (shown to premium users)
     */
    private lateinit var premiumCategory: PreferenceCategory
    /**
     * Category containing ways to become premium (shown to not premium users)
     */
    private lateinit var notPremiumCategory: PreferenceCategory
    /**
     * Is the premium category shown
     */
    private var premiumShown = true
    /**
     * Is the not premium category shown
     */
    private var notPremiumShown = true

    private val iab: Iab by inject()
    private val parameters: Parameters by inject()

// ---------------------------------------->

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Rating button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_rate_button_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let { activity ->
                RatingPopup(activity, parameters).show(true)
            }
            false
        }

        /*
         * Start day of week
         */
        val firstDayOfWeekPref = findPreference<SwitchPreferenceCompat>(getString(R.string.setting_category_start_day_of_week_key))
        firstDayOfWeekPref?.isChecked = parameters.getCaldroidFirstDayOfWeek() == CaldroidFragment.SUNDAY
        firstDayOfWeekPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            parameters.setCaldroidFirstDayOfWeek(if ((firstDayOfWeekPref?.isChecked) == true) CaldroidFragment.SUNDAY else CaldroidFragment.MONDAY)
            true
        }

        /*
         * Bind bug report button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_bug_report_send_button_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val localId = parameters.getLocalId()

            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SENDTO
            sendIntent.data = Uri.parse("mailto:") // only email apps should handle this
            sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.bug_report_email)))
            sendIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.setting_category_bug_report_send_text, localId))
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.setting_category_bug_report_send_subject))

            val packageManager = activity?.packageManager
            if (packageManager != null && sendIntent.resolveActivity(packageManager) != null) {
                startActivity(sendIntent)
            } else {
                Toast.makeText(activity, resources.getString(R.string.setting_category_bug_report_send_error), Toast.LENGTH_SHORT).show()
            }

            false
        }

        /*
         * Share app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_share_app_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.app_invite_message) + "\n" + "https://play.google.com/store/apps/details?id=com.benoitletondor.easybudgetapp")
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            } catch (e: Exception) {
                Logger.error("An error occurred during sharing app activity start", e)
            }

            false
        }

        /*
         * App version
         */
        val appVersionPreference = findPreference<Preference>(resources.getString(R.string.setting_category_app_version_key))
        appVersionPreference?.title = resources.getString(R.string.setting_category_app_version_title, BuildConfig.VERSION_NAME)
        appVersionPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://twitter.com/BenoitLetondor")
            activity?.startActivity(i)

            false
        }

        /*
         * Currency change button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_currency_change_button_key))?.let { currencyPreference ->
            currencyPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                selectCurrencyDialog = SelectCurrencyFragment()
                selectCurrencyDialog!!.show((activity as SettingsActivity).supportFragmentManager, "SelectCurrency")

                false
            }

            setCurrencyPreferenceTitle(currencyPreference)
        }


        /*
         * Warning limit button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_limit_set_button_key))?.let { limitWarningPreference ->
            limitWarningPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val dialogView = activity?.layoutInflater?.inflate(R.layout.dialog_set_warning_limit, null)
                val limitEditText = dialogView?.findViewById<View>(R.id.warning_limit) as EditText
                limitEditText.setText(parameters.getLowMoneyWarningAmount().toString())
                limitEditText.setSelection(limitEditText.text.length) // Put focus at the end of the text

                context?.let { context ->
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(R.string.adjust_limit_warning_title)
                    builder.setMessage(R.string.adjust_limit_warning_message)
                    builder.setView(dialogView)
                    builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    builder.setPositiveButton(R.string.ok) { _, _ ->
                        var limitString = limitEditText.text.toString()
                        if (limitString.trim { it <= ' ' }.isEmpty()) {
                            limitString = "0" // Set a 0 value if no value is provided (will lead to an error displayed to the user)
                        }

                        try {
                            val newLimit = Integer.valueOf(limitString)

                            // Invalid value, alert the user
                            if (newLimit <= 0) {
                                throw IllegalArgumentException("limit should be > 0")
                            }

                            parameters.setLowMoneyWarningAmount(newLimit)
                            setLimitWarningPreferenceTitle(limitWarningPreference)
                        } catch (e: Exception) {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.adjust_limit_warning_error_title)
                                .setMessage(resources.getString(R.string.adjust_limit_warning_error_message))
                                .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                                .show()
                        }
                    }

                    val dialog = builder.show()

                    // Directly show keyboard when the dialog pops
                    limitEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                        // Check if the device doesn't have a physical keyboard
                        if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        }
                    }
                }

                false
            }

            setLimitWarningPreferenceTitle(limitWarningPreference)
        }

        /*
         * Premium status
         */
        premiumCategory = findPreference(resources.getString(R.string.setting_category_premium_key))!!
        notPremiumCategory = findPreference(resources.getString(R.string.setting_category_not_premium_key))!!
        refreshPremiumPreference()

        /*
         * Notifications
         */
        val updateNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_update_key))
        updateNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            parameters.setUserAllowUpdatePushes((it as CheckBoxPreference).isChecked)
            true
        }
        updateNotifPref?.isChecked = parameters.isUserAllowingUpdatePushes()

        /*
         * Hide dev preferences if needed
         */
        val devCategory = findPreference<PreferenceCategory>(resources.getString(R.string.setting_category_dev_key))
        if (!BuildConfig.DEV_PREFERENCES) {
            preferenceScreen.removePreference(devCategory)
        } else {
            /*
             * Show welcome screen button
             */
            findPreference<Preference>(resources.getString(R.string.setting_category_show_welcome_screen_button_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                context?.let { context ->
                    LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(MainActivity.INTENT_SHOW_WELCOME_SCREEN))
                }

                activity?.finish()
                false
            }

            /*
             * Show premium screen
             */
            findPreference<Preference>(resources.getString(R.string.setting_category_dev_show_premium_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showBecomePremiumDialog()
                false
            }

            /*
             * Show dark theme notif
             */
            findPreference<Preference>(getString(R.string.setting_category_dev_show_dark_theme_notif_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                DarkThemeNotif.showDarkThemeNotif(context!!)
                false
            }
        }

        /*
         * Broadcast receiver
         */
        val filter = IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(USER_GONE_PREMIUM_INTENT)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(appContext: Context, intent: Intent) {
                if (SelectCurrencyFragment.CURRENCY_SELECTED_INTENT == intent.action && selectCurrencyDialog != null) {
                    findPreference<Preference>(resources.getString(R.string.setting_category_currency_change_button_key))?.let { currencyPreference ->
                        setCurrencyPreferenceTitle(currencyPreference)
                    }

                    selectCurrencyDialog!!.dismiss()
                    selectCurrencyDialog = null
                } else if (INTENT_IAB_STATUS_CHANGED == intent.action) {
                    try {
                        refreshPremiumPreference()
                    } catch (e: Exception) {
                        Logger.error("Error while receiving INTENT_IAB_STATUS_CHANGED intent", e)
                    }

                } else if (USER_GONE_PREMIUM_INTENT == intent.action) {
                    context?.let { context ->
                        AlertDialog.Builder(context)
                            .setTitle(R.string.iab_purchase_success_title)
                            .setMessage(R.string.iab_purchase_success_message)
                            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                    }

                    refreshPremiumPreference()
                }
            }
        }

        context?.let { context ->
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        }

        /*
         * Check if we should show premium popup
         */
        if (activity?.intent?.getBooleanExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, false) == true) {
            activity?.intent?.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, false)
            showBecomePremiumDialog()
        }

        /*
         * Check if we should show theme options
         */
        if( activity?.intent?.getBooleanExtra(SHOW_THEME_INTENT_KEY, false) == true ) {
            activity?.intent?.putExtra(SHOW_THEME_INTENT_KEY, false)
            onDisplayPreferenceDialog(findPreference<ListPreference>(getString(R.string.setting_category_app_theme_key)))
        }
    }

    /**
     * Set the currency preference title according to selected currency
     *
     * @param currencyPreference
     */
    private fun setCurrencyPreferenceTitle(currencyPreference: Preference) {
        currencyPreference.title = resources.getString(R.string.setting_category_currency_change_button_title, parameters.getUserCurrency().symbol)
    }

    /**
     * Set the limit warning preference title according to the selected limit
     *
     * @param limitWarningPreferenceTitle
     */
    private fun setLimitWarningPreferenceTitle(limitWarningPreferenceTitle: Preference) {
        limitWarningPreferenceTitle.title = resources.getString(R.string.setting_category_limit_set_button_title, CurrencyHelper.getFormattedCurrencyString(parameters, parameters.getLowMoneyWarningAmount().toDouble()))
    }

    /**
     * Show the right premium preference depending on the user state
     */
    private fun refreshPremiumPreference() {
        val isPremium = iab.isUserPremium()

        if (isPremium) {
            if (notPremiumShown) {
                preferenceScreen.removePreference(notPremiumCategory)
                notPremiumShown = false
            }

            if (!premiumShown) {
                preferenceScreen.addPreference(premiumCategory)
                premiumShown = true
            }

            // Premium preference
            findPreference<Preference>(resources.getString(R.string.setting_category_premium_status_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                context?.let {context ->
                    AlertDialog.Builder(context)
                        .setTitle(R.string.premium_popup_premium_title)
                        .setMessage(R.string.premium_popup_premium_message)
                        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }

                false
            }

            // Daily reminder notif preference
            val dailyNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_daily_key))
            dailyNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                parameters.setUserAllowDailyReminderPushes((it as CheckBoxPreference).isChecked)
                true
            }
            dailyNotifPref?.isChecked = parameters.isUserAllowingDailyReminderPushes()

            // Monthly reminder for reports
            val monthlyNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_monthly_key))
            monthlyNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                parameters.setUserAllowMonthlyReminderPushes((it as CheckBoxPreference).isChecked)
                true
            }
            monthlyNotifPref?.isChecked = parameters.isUserAllowingMonthlyReminderPushes()

            // Theme
            findPreference<ListPreference>(getString(R.string.setting_category_app_theme_key))?.let { themePref ->
                val currentTheme = parameters.getTheme()

                themePref.value = currentTheme.value.toString()
                themePref.summary = themePref.entries[themePref.findIndexOfValue(themePref.value)]
                themePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    themePref.summary = themePref.entries[themePref.findIndexOfValue(newValue as String)]

                    val newTheme = AppTheme.values().first { it.value == newValue.toInt() }

                    parameters.setTheme(newTheme)
                    AppCompatDelegate.setDefaultNightMode(newTheme.toPlatformValue())

                    true
                }
            }
        } else {
            if (premiumShown) {
                preferenceScreen.removePreference(premiumCategory)
                premiumShown = false
            }

            if (!notPremiumShown) {
                preferenceScreen.addPreference(notPremiumCategory)
                notPremiumShown = true
            }

            // Not premium preference
            findPreference<Preference>(resources.getString(R.string.setting_category_not_premium_status_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showBecomePremiumDialog()
                false
            }

            // Redeem promo code pref
            findPreference<Preference>(resources.getString(R.string.setting_category_premium_redeem_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    val dialogView = activity.layoutInflater.inflate(R.layout.dialog_redeem_voucher, null)
                    val voucherEditText = dialogView.findViewById<View>(R.id.voucher) as EditText

                    val builder = AlertDialog.Builder(activity)
                        .setTitle(R.string.voucher_redeem_dialog_title)
                        .setMessage(R.string.voucher_redeem_dialog_message)
                        .setView(dialogView)
                        .setPositiveButton(R.string.voucher_redeem_dialog_cta) { dialog, _ ->
                            dialog.dismiss()

                            val promocode = voucherEditText.text.toString()
                            if (promocode.trim { it <= ' ' }.isEmpty()) {
                                AlertDialog.Builder(activity)
                                    .setTitle(R.string.voucher_redeem_error_dialog_title)
                                    .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                                    .setPositiveButton(R.string.ok) { dialog12, _ -> dialog12.dismiss() }
                                    .show()
                            }

                            if (!launchRedeemPromocodeFlow(promocode)) {
                                AlertDialog.Builder(activity)
                                    .setTitle(R.string.iab_purchase_error_title)
                                    .setMessage(resources.getString(R.string.iab_purchase_error_message, "Error redeeming promo code"))
                                    .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                                    .show()
                            }
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

                    val dialog = builder.show()

                    // Directly show keyboard when the dialog pops
                    voucherEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                        // Check if the device doesn't have a physical keyboard
                        if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        }
                    }
                }

                false
            }
        }
    }

    private fun showBecomePremiumDialog() {
        activity?.let { activity ->
            val intent = Intent(activity, PremiumActivity::class.java)
            ActivityCompat.startActivityForResult(activity, intent, SettingsActivity.PREMIUM_ACTIVITY, null)
        }
    }

    /**
     * Launch the redeem promocode flow
     *
     * @param promocode the promocode to redeem
     */
    private fun launchRedeemPromocodeFlow(promocode: String): Boolean {
        return try {
            val url = "https://play.google.com/redeem?code=" + URLEncoder.encode(promocode, "UTF-8")
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: Exception) {
            Logger.error(false, "Error while redeeming promocode", e)
            false
        }

    }

    override fun onDestroy() {
        context?.let {context ->
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }

        super.onDestroy()
    }
}
