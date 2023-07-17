/*
 *   Copyright 2023 Benoit LETONDOR
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

import com.benoitletondor.easybudgetapp.view.welcome.WelcomeActivity
import androidx.core.app.ActivityCompat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityMainBinding

import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.account.AccountFragment
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
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
class MainActivity : BaseActivity<ActivityMainBinding>(), MenuProvider {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var parameters: Parameters

// ------------------------------------------>

    override fun createBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch welcome screen if needed
        if (parameters.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
            val startIntent = Intent(this, WelcomeActivity::class.java)
            ActivityCompat.startActivityForResult(this, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null)
        }

        setSupportActionBar(binding.toolbar)
        addMenuProvider(this)

        collectViewModelEvents()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WELCOME_SCREEN_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onWelcomeScreenFinished()
            } else if (resultCode == RESULT_CANCELED) {
                finish() // Finish activity if welcome screen is finish via back button
            }
        }
    }

    private fun collectViewModelEvents() {
        lifecycleScope.launchCollect(viewModel.premiumStatusFlow) {
            invalidateOptionsMenu()
        }

        lifecycleScope.launchCollect(viewModel.openPremiumEventFlow) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)
            ActivityCompat.startActivity(this, startIntent, null)
        }

        lifecycleScope.launchCollect(viewModel.accountSelectionFlow) { selectedAccount ->
            performIntentActionIfAny()

            when(selectedAccount) {
                MainViewModel.SelectedAccount.Loading -> Unit // TODO
                MainViewModel.SelectedAccount.Offline -> supportFragmentManager.commit {
                    replace(R.id.mainFragmentContainer, AccountFragment())
                }
                is MainViewModel.SelectedAccount.Online -> Unit // TODO
            }
        }
    }

    private fun performIntentActionIfAny() {
        if (intent != null && viewModel.accountSelectionFlow.value !== MainViewModel.SelectedAccount.Loading) {
            openSettingsIfNeeded(intent)
            openMonthlyReportIfNeeded(intent)
            openPremiumIfNeeded(intent)
            openAddExpenseIfNeeded(intent)
            openAddRecurringExpenseIfNeeded(intent)
            openSettingsForBackupIfNeeded(intent)
            intent = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        this.intent = intent
        performIntentActionIfAny()
    }

// ------------------------------------------>

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        if (viewModel.isIabReady) {
            // Remove monthly report for non premium users
            if ( !viewModel.isUserPremium ) {
                menu.removeItem(R.id.action_monthly_report)
            } else {
                menu.removeItem(R.id.action_become_premium)

                if ( !parameters.hasUserSawMonthlyReportHint() ) {
                    binding.monthlyReportHint.isVisible = true

                    binding.monthlyReportHintButton.setOnClickListener {
                        binding.monthlyReportHint.isVisible = false
                        parameters.setUserSawMonthlyReportHint()
                    }
                }
            }
        } else {
            menu.removeItem(R.id.action_become_premium)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_settings -> {
                val startIntent = Intent(this, SettingsActivity::class.java)
                ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)

                true
            }
            R.id.action_monthly_report -> {
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)

                true
            }
            R.id.action_become_premium -> {
                viewModel.onBecomePremiumButtonPressed()

                true
            }
            else -> false
        }
    }

// ------------------------------------------>

    /**
     * Open the settings activity if the given intent contains the [.INTENT_REDIRECT_TO_SETTINGS_EXTRA]
     * extra.
     */
    private fun openSettingsIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_EXTRA, false)) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
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
            ActivityCompat.startActivityForResult(this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
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

            ActivityCompat.startActivityForResult(this, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null)
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
            val startIntent = Intent(this, ExpenseEditActivity::class.java)
            startIntent.putExtra("date", LocalDate.now().toEpochDay())

            ActivityCompat.startActivityForResult(this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)
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
            val startIntent = Intent(this, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", LocalDate.now().toEpochDay())

            ActivityCompat.startActivityForResult(this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null)
        }
    }

    companion object {
        const val ADD_EXPENSE_ACTIVITY_CODE = 101
        const val MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE = 102
        const val WELCOME_SCREEN_ACTIVITY_CODE = 103
        const val SETTINGS_SCREEN_ACTIVITY_CODE = 104
        const val INTENT_EXPENSE_DELETED = "intent.expense.deleted"
        const val INTENT_RECURRING_EXPENSE_DELETED = "intent.expense.monthly.deleted"
        const val INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show"
        const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"
        const val INTENT_SHOW_CHECKED_BALANCE_CHANGED = "intent.showcheckedbalance.changed"
        const val INTENT_LOW_MONEY_WARNING_THRESHOLD_CHANGED = "intent.lowmoneywarningthreshold.changed"

        const val INTENT_REDIRECT_TO_PREMIUM_EXTRA = "intent.extra.premiumshow"
        const val INTENT_REDIRECT_TO_SETTINGS_EXTRA = "intent.extra.redirecttosettings"
        const val INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA = "intent.extra.redirecttosettingsforbackup"

        const val ANIMATE_TRANSITION_KEY = "animate"
        const val CENTER_X_KEY = "centerX"
        const val CENTER_Y_KEY = "centerY"
    }
}