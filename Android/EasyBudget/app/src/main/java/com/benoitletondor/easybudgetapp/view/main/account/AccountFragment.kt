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

package com.benoitletondor.easybudgetapp.view.main.account

import android.app.Dialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.FragmentAccountBinding
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.preventUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.helper.viewLifecycleScope
import com.benoitletondor.easybudgetapp.iab.INTENT_IAB_STATUS_CHANGED
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.account.calendar.CalendarView
import com.benoitletondor.easybudgetapp.view.main.manageaccount.ManageAccountActivity
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment
import com.benoitletondor.easybudgetapp.view.welcome.WelcomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment(), MenuProvider {
    private val viewModel: AccountViewModel by viewModels()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MainActivity.INTENT_EXPENSE_DELETED -> {
                    val expense = intent.getParcelableExtra<Expense>("expense")!!

                    viewModel.onDeleteExpenseClicked(expense)
                }
                MainActivity.INTENT_RECURRING_EXPENSE_DELETED -> {
                    val expense = intent.getParcelableExtra<Expense>("expense")!!
                    val deleteType = RecurringExpenseDeleteType.fromValue(intent.getIntExtra("deleteType", RecurringExpenseDeleteType.ALL.value))!!

                    viewModel.onDeleteRecurringExpenseClicked(expense, deleteType)
                }
                SelectCurrencyFragment.CURRENCY_SELECTED_INTENT -> viewModel.onCurrencySelected()
                MainActivity.INTENT_SHOW_WELCOME_SCREEN -> {
                    val startIntent = Intent(requireContext(), WelcomeActivity::class.java)
                    ActivityCompat.startActivityForResult(requireActivity(), startIntent,
                        MainActivity.WELCOME_SCREEN_ACTIVITY_CODE, null)
                }
                INTENT_IAB_STATUS_CHANGED -> viewModel.onIabStatusChanged()
                MainActivity.INTENT_SHOW_CHECKED_BALANCE_CHANGED -> viewModel.onShowCheckedBalanceChanged()
                MainActivity.INTENT_LOW_MONEY_WARNING_THRESHOLD_CHANGED -> viewModel.onLowMoneyWarningThresholdChanged()
            }
        }
    }

    private lateinit var balanceDateFormatter: DateTimeFormatter
    private lateinit var expensesViewAdapter: ExpensesRecyclerViewAdapter

    private val menuBackgroundAnimationDuration: Long = 150
    private var menuExpandAnimation: Animation? = null
    private var menuCollapseAnimation: Animation? = null

    private var isMenuExpended = false

    @Inject
    lateinit var parameters: Parameters
    @Inject
    lateinit var iab: Iab

    private var _binding: FragmentAccountBinding? = null
    private val binding: FragmentAccountBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAccountBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        balanceDateFormatter = DateTimeFormatter.ofPattern(resources.getString(R.string.account_balance_date_format), Locale.getDefault())

        initCalendarView()
        initFab()
        initRecyclerView()
        registerBroadcastReceiver()
        observeViewModel()

        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_account, menu)

        if (!viewModel.shouldShowPremiumRelatedButtons) {
            // Remove monthly report for non premium users
            menu.removeItem(R.id.action_monthly_report)
            menu.removeItem(R.id.action_check_all_past_entries)
        }

        // Remove back to today button if needed
        if (!viewModel.showGoToCurrentMonthButtonStateFlow.value) {
            menu.removeItem(R.id.action_go_to_current_month)
        }

        // Remove manage account if needed
        if (!viewModel.showManageAccountMenuItem.value) {
            menu.removeItem(R.id.action_manage_account)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.action_go_to_current_month -> {
                viewModel.onGoBackToCurrentMonthButtonPressed()
                true
            }
            R.id.action_balance -> {
                viewModel.onAdjustCurrentBalanceClicked()
                true
            }
            R.id.action_check_all_past_entries -> {
                viewModel.onCheckAllPastEntriesPressed()
                true
            }
            R.id.action_go_to_current_month -> {
                viewModel.onGoBackToCurrentMonthButtonPressed()
                true
            }
            R.id.action_monthly_report -> {
                viewModel.onMonthlyReportButtonPressed()
                true
            }
            R.id.action_manage_account -> {
                viewModel.onManageAccountButtonPressed()
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        _binding = null

        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleScope.launchCollect(viewModel.dbAvailableFlow) { dbState ->
            binding.accountLoadedView.isVisible = dbState is AccountViewModel.DBState.Loaded
            binding.accountLoadingView.isVisible = dbState is AccountViewModel.DBState.Loading
            binding.accountErrorView.isVisible = dbState is AccountViewModel.DBState.Error

            if (dbState is AccountViewModel.DBState.Error) {
                binding.accountErrorMessageTextView.text = getString(R.string.account_error_loading_message, dbState.error.localizedMessage)
                binding.accountErrorMessageRetryCta.setOnClickListener {
                    viewModel.onRetryLoadingButtonPressed()
                }
            }
        }

        viewLifecycleScope.launchCollect(viewModel.expenseDeletionSuccessEventFlow) { (deletedExpense, restoreAction) ->
            val snackbar = Snackbar.make(
                binding.coordinatorLayout,
                if (deletedExpense.isRevenue()) R.string.income_delete_snackbar_text else R.string.expense_delete_snackbar_text,
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onExpenseDeletionCancelled(restoreAction)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewLifecycleScope.launchCollect(viewModel.expenseDeletionErrorEventFlow) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.expense_delete_error_title)
                .setMessage(R.string.expense_delete_error_message)
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        var expenseDeletionDialog: ProgressDialog? = null
        viewLifecycleScope.launchCollect(viewModel.recurringExpenseDeletionProgressStateFlow) { state ->
            when(state) {
                is AccountViewModel.RecurringExpenseDeleteProgressState.Deleting -> {
                    val dialog = ProgressDialog(requireContext())
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_delete_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_delete_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseDeletionDialog = dialog
                }
                AccountViewModel.RecurringExpenseDeleteProgressState.Idle -> {
                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
            }
        }

        viewLifecycleScope.launchCollect(viewModel.recurringExpenseDeletionEventFlow) { event ->
            when(event) {
                is AccountViewModel.RecurringExpenseDeletionEvent.ErrorCantDeleteBeforeFirstOccurrence -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.recurring_expense_delete_first_error_title)
                        .setMessage(R.string.recurring_expense_delete_first_error_message)
                        .setNegativeButton(R.string.ok, null)
                        .show()
                }
                is AccountViewModel.RecurringExpenseDeletionEvent.ErrorIO -> {
                    showGenericRecurringDeleteErrorDialog()
                }
                is AccountViewModel.RecurringExpenseDeletionEvent.ErrorRecurringExpenseDeleteNotAssociated -> {
                    showGenericRecurringDeleteErrorDialog()
                }
                is AccountViewModel.RecurringExpenseDeletionEvent.Success -> {
                    val snackbar = Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_delete_success_message,
                        Snackbar.LENGTH_LONG
                    )

                    snackbar.setAction(R.string.undo) {
                        viewModel.onRestoreRecurringExpenseClicked(
                            event.recurringExpense,
                            event.restoreAction,
                        )
                    }

                    snackbar.setActionTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.snackbar_action_undo
                        )
                    )
                    snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
                    snackbar.show()
                }
            }
        }

        var expenseRestoreDialog: Dialog? = null
        viewLifecycleScope.launchCollect(viewModel.recurringExpenseRestoreProgressStateFlow) { state ->
            when(state) {
                AccountViewModel.RecurringExpenseRestoreProgressState.Idle -> {
                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
                is AccountViewModel.RecurringExpenseRestoreProgressState.Restoring -> {
                    val dialog = ProgressDialog(requireContext())
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_restoring_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_restoring_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseRestoreDialog = dialog
                }
            }
        }

        viewLifecycleScope.launchCollect(viewModel.recurringExpenseRestoreEventFlow) { event ->
            when(event) {
                is AccountViewModel.RecurringExpenseRestoreEvent.ErrorIO -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.recurring_expense_restore_error_title)
                        .setMessage(resources.getString(R.string.recurring_expense_restore_error_message))
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                is AccountViewModel.RecurringExpenseRestoreEvent.Success -> {
                    Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_restored_success_message,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewLifecycleScope.launchCollect(viewModel.startCurrentBalanceEditorEventFlow) { currentBalance ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_adjust_balance, null)
            val amountEditText = dialogView.findViewById<EditText>(R.id.balance_amount)
            amountEditText.setText(
                if (currentBalance == 0.0) "0" else CurrencyHelper.getFormattedAmountValue(
                    currentBalance
                )
            )
            amountEditText.preventUnsupportedInputForDecimals()
            amountEditText.setSelection(amountEditText.text.length) // Put focus at the end of the text

            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle(R.string.adjust_balance_title)
            builder.setMessage(R.string.adjust_balance_message)
            builder.setView(dialogView)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                try {
                    val stringValue = amountEditText.text.toString()
                    if (stringValue.isNotBlank()) {
                        val newBalance = java.lang.Double.valueOf(stringValue)
                        viewModel.onNewBalanceSelected(
                            newBalance,
                            getString(R.string.adjust_balance_expense_title)
                        )
                    }
                } catch (e: Exception) {
                    Logger.error("Error parsing new balance", e)
                }

                dialog.dismiss()
            }

            val dialog = builder.show()

            // Directly show keyboard when the dialog pops
            amountEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Check if the device doesn't have a physical keyboard
                if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        }

        viewLifecycleScope.launchCollect(viewModel.currentBalanceEditingErrorEventFlow) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.adjust_balance_error_title)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        }

        viewLifecycleScope.launchCollect(viewModel.currentBalanceEditedEventFlow) { (expense, diff, newBalance) ->
            //Show snackbar
            val snackbar = Snackbar.make(
                binding.coordinatorLayout,
                resources.getString(
                    R.string.adjust_balance_snackbar_text,
                    CurrencyHelper.getFormattedCurrencyString(parameters, newBalance)
                ),
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onCurrentBalanceEditedCancelled(expense, diff)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewLifecycleScope.launchCollect(viewModel.currentBalanceRestoringErrorEventFlow) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.adjust_balance_error_title)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        }

        viewLifecycleScope.launchCollect(viewModel.selectedDateDataFlow) { (date, balance, maybeCheckedBalance, expenses) ->
            refreshBalanceAndExpenseListForDate(date, balance, maybeCheckedBalance, expenses)
        }

        viewLifecycleScope.launchCollect(viewModel.premiumStatusFlow) {
            invalidateOptionsMenu(requireActivity())

            expensesViewAdapter.setUserPremium(isPremium = when(it) {
                PremiumCheckStatus.INITIALIZING,
                PremiumCheckStatus.CHECKING,
                PremiumCheckStatus.ERROR,
                PremiumCheckStatus.NOT_PREMIUM -> false
                PremiumCheckStatus.LEGACY_PREMIUM,
                PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                PremiumCheckStatus.PRO_SUBSCRIBED -> true
            })
        }

        viewLifecycleScope.launchCollect(viewModel.expenseCheckedErrorEventFlow) { exception ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.expense_check_error_title)
                .setMessage(
                    getString(
                        R.string.expense_check_error_message,
                        exception.localizedMessage
                    )
                )
                .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }

        viewLifecycleScope.launchCollect(viewModel.showGoToCurrentMonthButtonStateFlow) {
            invalidateOptionsMenu(requireActivity())
        }

        viewLifecycleScope.launchCollect(viewModel.showManageAccountMenuItem) {
            invalidateOptionsMenu(requireActivity())
        }

        viewLifecycleScope.launchCollect(viewModel.confirmCheckAllPastEntriesEventFlow) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.check_all_past_expences_title)
                .setMessage(getString(R.string.check_all_past_expences_message))
                .setPositiveButton(R.string.check_all_past_expences_confirm_cta) { dialog2, _ ->
                    viewModel.onCheckAllPastEntriesConfirmPressed()
                    dialog2.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }

        viewLifecycleScope.launchCollect(viewModel.checkAllPastEntriesErrorEventFlow) { error ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.check_all_past_expences_error_title)
                .setMessage(
                    getString(
                        R.string.check_all_past_expences_error_message,
                        error.localizedMessage
                    )
                )
                .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                .show()
        }

        viewLifecycleScope.launchCollect(viewModel.openMonthlyReportEventFlow) {
            val startIntent = Intent(requireActivity(), MonthlyReportBaseActivity::class.java)
            ActivityCompat.startActivity(requireContext(), startIntent, null)
        }

        viewLifecycleScope.launchCollect(viewModel.openManageAccountEventFlow) { account ->
            startActivity(ManageAccountActivity.newIntent(requireContext(), account))
        }

        viewLifecycleScope.launchCollect(viewModel.openExpenseAddEventFlow) { date ->
            val startIntent = ExpenseEditActivity.newIntent(
                context = requireContext(),
                editedExpense = null,
                date = date,
            )

            requireActivity().startActivity(startIntent)
        }
    }

    private fun registerBroadcastReceiver() {
        // Register receiver
        val filter = IntentFilter()
        filter.addAction(MainActivity.INTENT_EXPENSE_DELETED)
        filter.addAction(MainActivity.INTENT_RECURRING_EXPENSE_DELETED)
        filter.addAction(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        filter.addAction(MainActivity.INTENT_SHOW_WELCOME_SCREEN)
        filter.addAction(Intent.ACTION_VIEW)
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(MainActivity.INTENT_SHOW_CHECKED_BALANCE_CHANGED)
        filter.addAction(MainActivity.INTENT_LOW_MONEY_WARNING_THRESHOLD_CHANGED)

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
    }

// ------------------------------------------>

    /**
     * Update the balance for the given day
     * TODO optimization
     */
    private fun updateBalanceDisplayForDay(day: LocalDate, balance: Double, maybeCheckedBalance: Double?) {
        var formatted = resources.getString(R.string.account_balance_format, balanceDateFormatter.format(day))

        // FIXME it's ugly!!
        if (formatted.endsWith(".:")) {
            formatted = formatted.substring(0, formatted.length - 2) + ":" // Remove . at the end of the month (ex: nov.: -> nov:)
        } else if (formatted.endsWith(". :")) {
            formatted = formatted.substring(0, formatted.length - 3) + " :" // Remove . at the end of the month (ex: nov. : -> nov :)
        }

        binding.budgetLine.text = formatted
        binding.budgetLineAmount.text = if (maybeCheckedBalance != null) {
            resources.getString(
                R.string.account_balance_checked_format,
                CurrencyHelper.getFormattedCurrencyString(parameters, balance),
                CurrencyHelper.getFormattedCurrencyString(parameters, maybeCheckedBalance),
            )
        } else {
            CurrencyHelper.getFormattedCurrencyString(parameters, balance)
        }

        binding.budgetLineAmount.setTextColor(ContextCompat.getColor(requireContext(), when {
            balance <= 0 -> R.color.budget_red
            balance < parameters.getLowMoneyWarningAmount() -> R.color.budget_orange
            else -> R.color.budget_green
        }))
    }

    private fun initCalendarView() {
        binding.calendarView.setContent {
            AppTheme {
                CalendarView(
                    parameters = parameters,
                    dbAvailableFlow = viewModel.dbAvailableFlow,
                    forceRefreshDataFlow = viewModel.forceRefreshFlow,
                    selectedDateFlow = viewModel.selectDateFlow,
                    includeCheckedBalanceFlow = viewModel.includeCheckedBalanceFlow,
                    onMonthChanged = viewModel::onMonthChanged,
                    goBackToCurrentMonthEventFlow = viewModel.goBackToCurrentMonthEventFlow,
                    onDateSelected = viewModel::onSelectDate,
                    onDateLongClicked = viewModel::onDateLongClicked,
                )
            }
        }
    }

    private fun initFab() {
        isMenuExpended = binding.fabChoicesBackground.visibility == View.VISIBLE

        binding.fabChoicesBackground.setOnClickListener { collapseMenu() }
        binding.fabChoices.setOnClickListener {
            if (isMenuExpended) {
                collapseMenu()
            } else {
                expandMenu()
            }
        }

        listOf(binding.fabNewExpense, binding.fabNewExpenseText).forEach {
            it.setOnClickListener {
                val startIntent = ExpenseEditActivity.newIntent(
                    context = requireContext(),
                    editedExpense = null,
                    date = viewModel.selectDateFlow.value,
                )

                requireActivity().startActivity(startIntent)

                collapseMenu()
            }
        }

        listOf(binding.fabNewRecurringExpense, binding.fabNewRecurringExpenseText).forEach {
            it.setOnClickListener {
                val startIntent = RecurringExpenseEditActivity.newIntent(
                    context = requireContext(),
                    startDate = viewModel.selectDateFlow.value,
                    editedExpense = null,
                )

                requireActivity().startActivity(startIntent)

                collapseMenu()
            }
        }
    }

    private fun initRecyclerView() {
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        expensesViewAdapter = ExpensesRecyclerViewAdapter(this, parameters, LocalDate.now()) { expense, checked ->
            viewModel.onExpenseChecked(expense, checked)
        }
        binding.expensesRecyclerView.adapter = expensesViewAdapter
    }

    private fun collapseMenu() {
        isMenuExpended = false
        menuExpandAnimation?.cancel()
        menuCollapseAnimation?.cancel()

        menuCollapseAnimation = AlphaAnimation(1.0f, 0.0f)
        menuCollapseAnimation?.duration = menuBackgroundAnimationDuration
        menuCollapseAnimation?.fillAfter = true
        menuCollapseAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                binding.fabChoicesBackground.visibility = View.GONE
                binding.fabChoicesBackground.isClickable = false

                binding.fabNewRecurringExpenseContainer.isVisible = false
                binding.fabNewExpenseContainer.isVisible = false
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        binding.fabChoicesBackground.startAnimation(menuCollapseAnimation)
        binding.fabNewRecurringExpense.startAnimation(menuCollapseAnimation)
        binding.fabNewRecurringExpenseText.startAnimation(menuCollapseAnimation)
        binding.fabNewExpense.startAnimation(menuCollapseAnimation)
        binding.fabNewExpenseText.startAnimation(menuCollapseAnimation)
    }

    private fun expandMenu() {
        isMenuExpended = true
        menuExpandAnimation?.cancel()
        menuCollapseAnimation?.cancel()

        menuExpandAnimation = AlphaAnimation(0.0f, 1.0f)
        menuExpandAnimation?.duration = menuBackgroundAnimationDuration
        menuExpandAnimation?.fillAfter = true
        menuExpandAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.fabChoicesBackground.visibility = View.VISIBLE
                binding.fabChoicesBackground.isClickable = true

                binding.fabNewRecurringExpenseContainer.isVisible = true
                binding.fabNewExpenseContainer.isVisible = true
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        binding.fabChoicesBackground.startAnimation(menuExpandAnimation)
        binding.fabNewRecurringExpense.startAnimation(menuExpandAnimation)
        binding.fabNewRecurringExpenseText.startAnimation(menuExpandAnimation)
        binding.fabNewExpense.startAnimation(menuExpandAnimation)
        binding.fabNewExpenseText.startAnimation(menuExpandAnimation)
    }

    private fun refreshRecyclerViewForDate(date: LocalDate, expenses: List<Expense>) {
        expensesViewAdapter.setDate(date, expenses)

        if ( expenses.isNotEmpty() ) {
            binding.expensesRecyclerView.visibility = View.VISIBLE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.GONE
        } else {
            binding.expensesRecyclerView.visibility = View.GONE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun refreshBalanceAndExpenseListForDate(date: LocalDate, balance: Double, maybeCheckedBalance: Double?, expenses: List<Expense>) {
        refreshRecyclerViewForDate(date, expenses)
        updateBalanceDisplayForDay(date, balance, maybeCheckedBalance)
    }

    /**
     * Show a generic alert dialog telling the user an error occured while deleting recurring expense
     */
    private fun showGenericRecurringDeleteErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recurring_expense_delete_error_title)
            .setMessage(R.string.recurring_expense_delete_error_message)
            .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        const val ARG_SELECTED_ACCOUNT = "selected_account"

        fun newInstance(account: MainViewModel.SelectedAccount.Selected): AccountFragment {
            return AccountFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SELECTED_ACCOUNT, account)
                }
            }
        }
    }
}