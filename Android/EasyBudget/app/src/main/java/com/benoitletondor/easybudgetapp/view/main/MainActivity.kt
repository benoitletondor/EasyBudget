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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityMainBinding
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.account.AccountFragment
import com.benoitletondor.easybudgetapp.view.main.accountselector.AccountSelectorFragment
import com.benoitletondor.easybudgetapp.view.main.loading.LoadingFragment
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

        binding.mainComposeView.setContent {
            val selectedAccount by viewModel.accountSelectionFlow.collectAsState()
            val hasPendingInvitations by viewModel.hasPendingInvitationsFlow.collectAsState()

            AppTheme {
                if (selectedAccount is MainViewModel.SelectedAccount.Selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(R.color.status_bar_color))
                            .padding(bottom = 8.dp)
                            .clickable(
                                onClick = viewModel::onAccountTapped,
                            )
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                            ) {
                                when(val account = selectedAccount) {
                                    MainViewModel.SelectedAccount.Loading -> Unit /* Nothing to display when loading */
                                    is MainViewModel.SelectedAccount.Selected -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.main_account_name) + " ",
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorResource(R.color.action_bar_text_color),
                                            )

                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = when(account) {
                                                    MainViewModel.SelectedAccount.Selected.Offline -> stringResource(R.string.main_account_default_name)
                                                    is MainViewModel.SelectedAccount.Selected.Online -> stringResource(R.string.main_account_online_name, account.name)
                                                },
                                                maxLines = 1,
                                                color = colorResource(R.color.action_bar_text_color),
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasPendingInvitations && selectedAccount is MainViewModel.SelectedAccount.Selected) {
                                Box(
                                    modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                                ){
                                    Image(
                                        painter =  painterResource(id = R.drawable.ic_baseline_notifications_24),
                                        colorFilter = ColorFilter.tint(colorResource(R.color.action_bar_text_color)),
                                        contentDescription = stringResource(R.string.account_pending_invitation_description),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colorResource(R.color.budget_red))
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            } else {
                                Image(
                                    painter =  painterResource(id = R.drawable.ic_baseline_arrow_drop_down_24),
                                    colorFilter = ColorFilter.tint(colorResource(R.color.action_bar_text_color)),
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WELCOME_SCREEN_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onWelcomeScreenFinished()
            } else if (resultCode == RESULT_CANCELED) {
                finish() // Finish activity if welcome screen is finish via back button
            }
        } else {
            for (fragment in supportFragmentManager.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
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
            invalidateOptionsMenu()

            withStarted {
                when(selectedAccount) {
                    MainViewModel.SelectedAccount.Loading -> {
                        supportFragmentManager.commit {
                            replace(R.id.mainFragmentContainer, LoadingFragment())
                        }
                    }
                    is MainViewModel.SelectedAccount.Selected -> {
                        performIntentActionIfAny()

                        supportFragmentManager.commit {
                            replace(R.id.mainFragmentContainer, AccountFragment.newInstance(selectedAccount))
                        }
                    }
                }
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                MainViewModel.Event.ShowAccountSelect -> AccountSelectorFragment().show(supportFragmentManager, "accountSelector")
            }
        }
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

        (viewModel.accountSelectionFlow.value as? MainViewModel.SelectedAccount.Selected)?.let {
            performIntentActionIfAny()
        }
    }

    fun onAccountSelectedFromBottomSheet(account: MainViewModel.SelectedAccount.Selected) {
        viewModel.onAccountSelected(account)
    }

// ------------------------------------------>

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        if (viewModel.shouldShowMenuButtons()) {
            // Remove monthly report for non premium users
            if ( viewModel.showPremiumMenuButtons() ) {
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
                this@MainActivity.startActivity(startIntent)

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
            AccountSelectorFragment().show(supportFragmentManager, "accountSelector")
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
        const val INTENT_OPEN_ACCOUNTS_TRAY_EXTRA = "intent.extra.openaccountstray"
    }
}