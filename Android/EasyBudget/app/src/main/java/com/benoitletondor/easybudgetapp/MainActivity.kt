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

package com.benoitletondor.easybudgetapp

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.compose.AppNavHost
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getNumberOfDailyOpen
import com.benoitletondor.easybudgetapp.parameters.getPremiumPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.getRatingPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.getTheme
import com.benoitletondor.easybudgetapp.parameters.hasPremiumPopupBeenShow
import com.benoitletondor.easybudgetapp.parameters.hasUserCompleteRating
import com.benoitletondor.easybudgetapp.parameters.setPremiumPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.setPremiumPopupShown
import com.benoitletondor.easybudgetapp.parameters.setRatingPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.getRatingPopupUserStep
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Main activity of the app
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var parameters: Parameters
    @Inject lateinit var iab: Iab

    private val openSubscriptionScreenLiveFlow = MutableLiveFlow<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
                detectDarkMode = { resources ->
                    // This is reversed to have the right bars colors
                    parameters.getTheme() == AppTheme.LIGHT ||
                            (parameters.getTheme() == AppTheme.PLATFORM_DEFAULT && (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO)
                }),
        )

        setContent {
            AppTheme {
                AppNavHost(
                    closeApp = {
                        finish()
                    },
                    openSubscriptionScreenFlow = openSubscriptionScreenLiveFlow,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        showPremiumPopupIfNeeded()
        showRatingPopupIfNeeded()
    }

    private fun showPremiumPopupIfNeeded() {
        lifecycleScope.launch {
            try {
                if ( parameters.hasPremiumPopupBeenShow() ) {
                    return@launch
                }

                if ( iab.isUserPremium() || iab.iabStatusFlow.value == PremiumCheckStatus.ERROR ) {
                    return@launch
                }

                if ( !parameters.hasUserCompleteRating() ) {
                    return@launch
                }

                val currentStep = parameters.getRatingPopupUserStep()
                if (currentStep == RatingPopup.RatingPopupStep.STEP_LIKE ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_NOT_RATED ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_RATED) {
                    if ( !hasRatingPopupBeenShownToday() && shouldShowPremiumPopup() ) {
                        parameters.setPremiumPopupLastAutoShowTimestamp(Date().time)

                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(R.string.premium_popup_become_title)
                                .setMessage(R.string.premium_popup_become_message)
                                .setPositiveButton(R.string.premium_popup_become_cta) { dialog13, _ ->
                                    lifecycleScope.launch {
                                        openSubscriptionScreenLiveFlow.emit(Unit)
                                    }

                                    dialog13.dismiss()
                                }
                                .setNegativeButton(R.string.premium_popup_become_not_now) { dialog12, _ -> dialog12.dismiss() }
                                .setNeutralButton(R.string.premium_popup_become_not_ask_again) { dialog1, _ ->
                                    parameters.setPremiumPopupShown()
                                    dialog1.dismiss()
                                }
                                .show()
                                .centerButtons()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error("Error while showing become premium popup", e)
            }
        }
    }

    /**
     * Show the rating popup if the user didn't asked not to every day after the app has been open
     * in 3 different days.
     */
    private fun showRatingPopupIfNeeded() {
        try {
            val dailyOpens = parameters.getNumberOfDailyOpen()
            if (dailyOpens > 2) {
                if (!hasRatingPopupBeenShownToday()) {
                    val shown = RatingPopup(this, parameters).show(false)
                    if (shown) {
                        parameters.setRatingPopupLastAutoShowTimestamp(Date().time)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("Error while showing rating popup", e)
        }

    }

    /**
     * Has the rating popup been shown automatically today
     *
     * @return true if the rating popup has been shown today, false otherwise
     */
    private fun hasRatingPopupBeenShownToday(): Boolean {
        val lastRatingTS = parameters.getRatingPopupLastAutoShowTimestamp()
        if (lastRatingTS > 0) {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = Date(lastRatingTS)
            val lastTimeDay = cal.get(Calendar.DAY_OF_YEAR)

            return currentDay == lastTimeDay
        }

        return false
    }

    /**
     * Check that last time the premium popup was shown was 2 days ago or more
     *
     * @return true if we can show premium popup, false otherwise
     */
    private fun shouldShowPremiumPopup(): Boolean {
        val lastPremiumTS = parameters.getPremiumPopupLastAutoShowTimestamp()
        if (lastPremiumTS == 0L) {
            return true
        }

        // Set calendar to last time 00:00 + 2 days
        val cal = Calendar.getInstance()
        cal.time = Date(lastPremiumTS)
        cal.set(Calendar.HOUR, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 2)

        return Date().after(cal.time)
    }

    private fun performIntentActionIfAny() {
        if (intent != null) {
            openSettingsIfNeeded(intent)
            openMonthlyReportIfNeeded(intent)
            openPremiumIfNeeded(intent)
            openAddExpenseIfNeeded(intent)
            openAddRecurringExpenseIfNeeded(intent)
            openSettingsForBackupIfNeeded(intent)
            openAccountsTrayIfNeeded(intent)
            intent = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent

        /*(viewModel.accountSelectionFlow.value as? MainViewModel.SelectedAccount.Selected)?.let {
            performIntentActionIfAny()
        }*/
    }

// ------------------------------------------>

    /**
     * Open the settings activity if the given intent contains the [.INTENT_REDIRECT_TO_SETTINGS_EXTRA]
     * extra.
     */
    private fun openSettingsIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_EXTRA, false)) {
            //val startIntent = Intent(this, SettingsActivity::class.java)
            //this@MainActivity.startActivity(startIntent)
        }
    }

    /**
     * Open the settings activity to display backup options if the given intent contains the
     * [.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA] extra.
     */
    private fun openSettingsForBackupIfNeeded(intent: Intent) {
        if( intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, false) ) {
            /*val startIntent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SHOW_BACKUP_INTENT_KEY, true)
            }
            this@MainActivity.startActivity(startIntent)*/
        }
    }

    private fun openAccountsTrayIfNeeded(intent: Intent) {
        if( intent.getBooleanExtra(INTENT_OPEN_ACCOUNTS_TRAY_EXTRA, false) ) {
            //AccountSelectorFragment().show(supportFragmentManager, "accountSelector")
            TODO()
        }
    }

    /**
     * Open the monthly report activity if the given intent contains the monthly uri part.
     *
     * @param intent
     */
    private fun openMonthlyReportIfNeeded(intent: Intent) {
        try {
            val data = intent.data
            if (data != null && "true" == data.getQueryParameter("monthly")) {
                /*val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, true)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)*/
                // FIXME
            }
        } catch (e: Exception) {
            Logger.error("Error while opening report activity", e)
        }
    }

    /**
     * Open the premium screen if the given intent contains the [.INTENT_REDIRECT_TO_PREMIUM_EXTRA]
     * extra.
     *
     * @param intent
     */
    private fun openPremiumIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_PREMIUM_EXTRA, false)) {
            /*val startIntent = Intent(this, SettingsActivity::class.java)
            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)

            this.startActivity(startIntent)*/
        }
    }

    /**
     * Open the add expense screen if the given intent contains the [.INTENT_SHOW_ADD_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_EXPENSE, false)) {
            val startIntent = ExpenseEditActivity.newIntent(
                context = this,
                date = LocalDate.now(),
                editedExpense = null,
            )

            startActivity(startIntent)
        }
    }

    /**
     * Open the add recurring expense screen if the given intent contains the [.INTENT_SHOW_ADD_RECURRING_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddRecurringExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_RECURRING_EXPENSE, false)) {
            val startIntent = RecurringExpenseEditActivity.newIntent(
                context = this,
                startDate = LocalDate.now(),
                editedExpense = null,
            )

            startActivity(startIntent)
        }
    }

    companion object {
        const val INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show"
        const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"
        const val INTENT_SHOW_CHECKED_BALANCE_CHANGED = "intent.showcheckedbalance.changed"
        const val INTENT_LOW_MONEY_WARNING_THRESHOLD_CHANGED = "intent.lowmoneywarningthreshold.changed"

        const val INTENT_REDIRECT_TO_PREMIUM_EXTRA = "intent.extra.premiumshow"
        const val INTENT_REDIRECT_TO_SETTINGS_EXTRA = "intent.extra.redirecttosettings"
        const val INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA = "intent.extra.redirecttosettingsforbackup"
        const val INTENT_OPEN_ACCOUNTS_TRAY_EXTRA = "intent.extra.openaccountstray"
    }
}