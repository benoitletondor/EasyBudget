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

package com.benoitletondor.easybudgetapp.view.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppNavHost
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
import com.benoitletondor.easybudgetapp.view.welcome.WelcomeActivity
import com.benoitletondor.easybudgetapp.view.welcome.getOnboardingStep
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var parameters: Parameters

// ------------------------------------------>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // Launch welcome screen if needed
        if (parameters.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
            val startIntent = Intent(this, WelcomeActivity::class.java)
            ActivityCompat.startActivityForResult(this, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null)
        }

        setContent {
            AppTheme {
                AppNavHost()
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*if (requestCode == WELCOME_SCREEN_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onWelcomeScreenFinished()
            } else if (resultCode == RESULT_CANCELED) {
                finish() // Finish activity if welcome screen is finish via back button
            }
        } else {
            for (fragment in supportFragmentManager.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }*/
    }

    private fun collectViewModelEvents() {
        /*

        lifecycleScope.launchCollect(viewModel.openPremiumEventFlow) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)
            ActivityCompat.startActivity(this, startIntent, null)
        }*/


        /*lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                MainViewModel.Event.ShowAccountSelect -> AccountSelectorFragment().show(supportFragmentManager, "accountSelector")
            }
        }*/
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
            val startIntent = Intent(this, SettingsActivity::class.java)
            this@MainActivity.startActivity(startIntent)
        }
    }

    /**
     * Open the settings activity to display backup options if the given intent contains the
     * [.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA] extra.
     */
    private fun openSettingsForBackupIfNeeded(intent: Intent) {
        if( intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, false) ) {
            val startIntent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SHOW_BACKUP_INTENT_KEY, true)
            }
            this@MainActivity.startActivity(startIntent)
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
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, true)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
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
            val startIntent = Intent(this, SettingsActivity::class.java)
            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)

            this.startActivity(startIntent)
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
        const val WELCOME_SCREEN_ACTIVITY_CODE = 103
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