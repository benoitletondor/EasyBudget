/*
 *   Copyright 2024 Benoit LETONDOR
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

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.AppTheme
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.getUserCurrency
import com.benoitletondor.easybudgetapp.iab.INTENT_IAB_STATUS_CHANGED
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.getLocalId
import com.benoitletondor.easybudgetapp.parameters.getLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.getShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.parameters.getTheme
import com.benoitletondor.easybudgetapp.parameters.isBackupEnabled
import com.benoitletondor.easybudgetapp.parameters.isUserAllowingDailyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.isUserAllowingMonthlyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.isUserAllowingUpdatePushes
import com.benoitletondor.easybudgetapp.parameters.setFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.setLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.setShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.parameters.setTheme
import com.benoitletondor.easybudgetapp.parameters.setUserAllowDailyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.setUserAllowMonthlyReminderPushes
import com.benoitletondor.easybudgetapp.parameters.setUserAllowUpdatePushes
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.main.createaccount.CreateAccountActivity
import com.benoitletondor.easybudgetapp.view.main.login.LoginActivity
import com.benoitletondor.easybudgetapp.view.premium.PremiumActivity
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.USER_GONE_PREMIUM_INTENT
import com.benoitletondor.easybudgetapp.view.settings.backup.BackupSettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
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
     * Launcher for notification permission request
     */
    private lateinit var notificationRequestPermissionLauncher: ActivityResultLauncher<String>
    /**
     * Is the premium category shown
     */
    private var premiumShown = true
    /**
     * Is the not premium category shown
     */
    private var notPremiumShown = true

    @Inject lateinit var iab: Iab
    @Inject lateinit var parameters: Parameters

// ---------------------------------------->

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationRequestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                activity?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.setting_notification_permission_rejected_dialog_title)
                        .setMessage(R.string.setting_notification_permission_rejected_dialog_description)
                        .setPositiveButton(R.string.setting_notification_permission_rejected_dialog_accept_cta) { dialog, _ ->
                            dialog.dismiss()
                            showNotificationPermissionIfNeeded()
                        }
                        .setNegativeButton(R.string.setting_notification_permission_rejected_dialog_not_now_cta) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

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
        firstDayOfWeekPref?.isChecked = parameters.getFirstDayOfWeek() == DayOfWeek.SUNDAY
        firstDayOfWeekPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            parameters.setFirstDayOfWeek(if ((firstDayOfWeekPref?.isChecked) == true) DayOfWeek.SUNDAY else DayOfWeek.MONDAY)
            true
        }

        /*
         * Backup
         */
        findPreference<Preference>(getString(R.string.setting_category_backup))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context, BackupSettingsActivity::class.java))
            false
        }
        updateBackupPreferences()

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
                    val builder = MaterialAlertDialogBuilder(context)
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
                            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(MainActivity.INTENT_LOW_MONEY_WARNING_THRESHOLD_CHANGED))
                        } catch (e: Exception) {
                            MaterialAlertDialogBuilder(context)
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
         * Show checked balance
         */
        val showCheckedBalancePref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_show_checked_balance_key))
        showCheckedBalancePref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            parameters.setShouldShowCheckedBalance((it as CheckBoxPreference).isChecked)

            context?.let { context ->
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(MainActivity.INTENT_SHOW_CHECKED_BALANCE_CHANGED))
            }

            true
        }
        showCheckedBalancePref?.isChecked = parameters.getShouldShowCheckedBalance()

        /*
         * Notifications
         */
        val updateNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_update_key))
        updateNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val checked = (it as CheckBoxPreference).isChecked
            parameters.setUserAllowUpdatePushes(checked)

            if (checked) {
                showNotificationPermissionIfNeeded()
            }

            true
        }
        updateNotifPref?.isChecked = parameters.isUserAllowingUpdatePushes()

        /*
         * Hide dev preferences if needed
         */
        val devCategory = findPreference<PreferenceCategory>(resources.getString(R.string.setting_category_dev_key))
        if (!BuildConfig.DEV_PREFERENCES) {
            devCategory?.let { preferenceScreen.removePreference(it) }
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
                showBecomePremiumActivity()
                false
            }

            findPreference<Preference>(getString(R.string.setting_category_dev_show_login))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    val intent = LoginActivity.newIntent(activity, shouldDismissAfterAuth = false)
                    activity.startActivity(intent)
                }
                false
            }

            findPreference<Preference>(getString(R.string.setting_category_dev_show_create_account))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    val intent = Intent(activity, CreateAccountActivity::class.java)
                    activity.startActivity(intent)
                }
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
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.iab_purchase_success_title)
                            .setMessage(R.string.iab_purchase_success_message)
                            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .setOnDismissListener {
                                showNotificationPermissionIfNeeded()
                            }
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
            showBecomePremiumActivity()
        }

        /*
         * Check if we should show pro popup
         */
        if (activity?.intent?.getBooleanExtra(SettingsActivity.SHOW_PRO_INTENT_KEY, false) == true) {
            activity?.intent?.putExtra(SettingsActivity.SHOW_PRO_INTENT_KEY, false)
            showBecomeProActivity()
        }

        /*
         * Check if we should show backup options
         */
        if( activity?.intent?.getBooleanExtra(SHOW_BACKUP_INTENT_KEY, false) == true ) {
            activity?.intent?.putExtra(SHOW_BACKUP_INTENT_KEY, false)
            startActivity(Intent(context, BackupSettingsActivity::class.java))
        }
    }

    private fun updateBackupPreferences() {
        findPreference<Preference>(getString(R.string.setting_category_backup))?.setSummary(if( parameters.isBackupEnabled() ) {
            R.string.backup_settings_backups_activated
        } else {
            R.string.backup_settings_backups_deactivated
        })
    }

    private fun showNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()

        updateBackupPreferences()
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
        lifecycleScope.launch {
            val isPremium = iab.isUserPremium()
            val isPro = iab.isUserPro()

            if (isPremium) {
                if (notPremiumShown) {
                    preferenceScreen.removePreference(notPremiumCategory)
                    notPremiumShown = false
                }

                if (!premiumShown) {
                    preferenceScreen.addPreference(premiumCategory)
                    premiumShown = true
                }

                // Premium/Pro preference
                findPreference<Preference>(resources.getString(R.string.setting_category_premium_status_key))?.let {
                    it.title = if (isPro) { getString(R.string.setting_category_pro_status_title)} else { getString(R.string.setting_category_premium_status_title) }
                    it.summary = if (isPro) { getString(R.string.setting_category_pro_status_message)} else { getString(R.string.setting_category_premium_status_message) }
                    it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        context?.let { context ->
                            if (!isPro) {
                                showBecomeProActivity()
                            } else {
                                MaterialAlertDialogBuilder(context)
                                    .setTitle(R.string.pro_popup_title)
                                    .setMessage(R.string.pro_popup_message)
                                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                    .show()
                            }

                        }

                        false
                    }
                }

                // Daily reminder notif preference
                val dailyNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_daily_key))
                dailyNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val checked = (it as CheckBoxPreference).isChecked
                    parameters.setUserAllowDailyReminderPushes(checked)

                    if (checked) {
                        showNotificationPermissionIfNeeded()
                    }

                    true
                }
                dailyNotifPref?.isChecked = parameters.isUserAllowingDailyReminderPushes()

                // Monthly reminder for reports
                val monthlyNotifPref = findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_monthly_key))
                monthlyNotifPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val checked = (it as CheckBoxPreference).isChecked
                    parameters.setUserAllowMonthlyReminderPushes(checked)

                    if (checked) {
                        showNotificationPermissionIfNeeded()
                    }

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

                        val newTheme = AppTheme.entries.first { it.value == newValue.toInt() }

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
                    showBecomePremiumActivity()
                    false
                }

                // Redeem promo code pref
                findPreference<Preference>(resources.getString(R.string.setting_category_premium_redeem_key))?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    activity?.let { activity ->
                        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_redeem_voucher, null)
                        val voucherEditText = dialogView.findViewById<View>(R.id.voucher) as EditText

                        val builder = MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.voucher_redeem_dialog_title)
                            .setMessage(R.string.voucher_redeem_dialog_message)
                            .setView(dialogView)
                            .setPositiveButton(R.string.voucher_redeem_dialog_cta) { dialog, _ ->
                                dialog.dismiss()

                                val promocode = voucherEditText.text.toString()
                                if (promocode.trim { it <= ' ' }.isEmpty()) {
                                    MaterialAlertDialogBuilder(activity)
                                        .setTitle(R.string.voucher_redeem_error_dialog_title)
                                        .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                                        .setPositiveButton(R.string.ok) { dialog12, _ -> dialog12.dismiss() }
                                        .show()
                                }

                                if (!launchRedeemPromocodeFlow(promocode)) {
                                    MaterialAlertDialogBuilder(activity)
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
    }

    private fun showBecomePremiumActivity() {
        activity?.let { activity ->
            val intent = PremiumActivity.createIntent(activity, shouldShowProByDefault = false)
            ActivityCompat.startActivityForResult(activity, intent, SettingsActivity.PREMIUM_ACTIVITY, null)
        }
    }

    private fun showBecomeProActivity() {
        activity?.let { activity ->
            val intent = PremiumActivity.createIntent(activity, shouldShowProByDefault = true)
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
            Logger.error("Error while redeeming promocode", e)
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
