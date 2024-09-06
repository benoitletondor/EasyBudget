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

import android.app.ProgressDialog
import android.content.res.Configuration
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.preventUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.view.main.subviews.accountselector.AccountSelectorView
import com.benoitletondor.easybudgetapp.view.main.subviews.FABMenuOverlay
import com.benoitletondor.easybudgetapp.view.main.subviews.MainViewContent
import com.benoitletondor.easybudgetapp.view.main.subviews.MainViewTopBar
import com.benoitletondor.easybudgetapp.view.main.subviews.MonthlyReportHint
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kizitonwose.calendar.core.atStartOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency

@Serializable
object MainDestination

@Composable
fun MainView(
    viewModel: MainViewModel = hiltViewModel(),
    openAddExpenseScreenLiveFlow: Flow<Unit>,
    openAddRecurringExpenseScreenLiveFlow: Flow<Unit>,
    openMonthlyReportScreenFromNotificationFlow: Flow<Unit>,
    navigateToOnboarding: () -> Unit,
    onboardingResultFlow: Flow<OnboardingResult>,
    closeApp: () -> Unit,
    navigateToPremium: (startOnPro: Boolean) -> Unit,
    navigateToMonthlyReport: (fromNotification: Boolean) -> Unit,
    navigateToManageAccount: (account: MainViewModel.SelectedAccount.Selected.Online) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToLogin: (shouldDismissAfterAuth: Boolean) -> Unit,
    navigateToCreateAccount: () -> Unit,
    navigateToAddExpense: (LocalDate, Expense?) -> Unit,
    navigateToAddRecurringExpense: (LocalDate, Expense?) -> Unit,
) {
    MainView(
        selectedAccountFlow = viewModel.accountSelectionFlow,
        dbStateFlow = viewModel.dbAvailableFlow,
        eventFlow = viewModel.eventFlow,
        showMonthlyReportHintFlow = viewModel.showMonthlyReportHintFlow,
        openAddExpenseScreenLiveFlow = openAddExpenseScreenLiveFlow,
        openAddRecurringExpenseScreenLiveFlow = openAddRecurringExpenseScreenLiveFlow,
        openMonthlyReportScreenFromNotificationFlow = openMonthlyReportScreenFromNotificationFlow,
        forceRefreshDataFlow = viewModel.forceRefreshFlow,
        firstDayOfWeekFlow = viewModel.firstDayOfWeekFlow,
        includeCheckedBalanceFlow = viewModel.includeCheckedBalanceFlow,
        getDataForMonth = viewModel::getDataForMonth,
        selectedDateFlow = viewModel.selectedDateFlow,
        lowMoneyAmountWarningFlow = viewModel.lowMoneyAmountWarningFlow,
        goBackToCurrentMonthEventFlow = viewModel.eventFlow
            .filterIsInstance<MainViewModel.Event.GoBackToCurrentMonth>()
            .map { /* No-op to produce Unit */ },
        appInitDate = viewModel.appInitDate,
        showActionButtonsFlow = viewModel.showMenuActionButtonsFlow,
        showPremiumRelatedButtonsFlow = viewModel.showPremiumRelatedButtonsFlow,
        showManageAccountButtonFlow = viewModel.showManageAccountMenuItemFlow,
        showGoBackToCurrentMonthButtonFlow = viewModel.showGoToCurrentMonthButtonStateFlow,
        hasPendingInvitationsFlow = viewModel.hasPendingInvitationsFlow,
        userCurrencyFlow = viewModel.userCurrencyFlow,
        recurringExpenseDeletionProgressFlow = viewModel.recurringExpenseDeletionProgressStateFlow,
        recurringExpenseRestoreProgressFlow = viewModel.recurringExpenseRestoreProgressStateFlow,
        dayDataFlow = viewModel.selectedDateDataFlow,
        showExpensesCheckBoxFlow = viewModel.showExpensesCheckBoxFlow,
        onboardingResultFlow = onboardingResultFlow,
        shouldNavigateToOnboarding = viewModel.shouldNavigateToOnboarding,
        onSettingsButtonPressed = viewModel::onSettingsButtonPressed,
        onAdjustCurrentBalanceButtonPressed = viewModel::onAdjustCurrentBalanceClicked,
        onTickAllPastEntriesButtonPressed = viewModel::onCheckAllPastEntriesPressed,
        onManageAccountButtonPressed = viewModel::onManageAccountButtonPressed,
        onDiscoverPremiumButtonPressed = viewModel::onDiscoverPremiumButtonPressed,
        onMonthlyReportButtonPressed = viewModel::onMonthlyReportButtonPressed,
        onGoBackToCurrentMonthButtonPressed = viewModel::onGoBackToCurrentMonthButtonPressed,
        onCurrentAccountTapped = viewModel::onCurrentAccountTapped,
        onMonthChanged = viewModel::onMonthChanged,
        onDateClicked = viewModel::onSelectDate,
        onDateLongClicked = viewModel::onDateLongClicked,
        onRetryDBLoadingButtonPressed = viewModel::onRetryLoadingDBButtonPressed,
        onAccountSelected = viewModel::onAccountSelected,
        onExpenseDeletionCancelled = viewModel::onExpenseDeletionCancelled,
        onCurrentBalanceEditedCancelled = viewModel::onCurrentBalanceEditedCancelled,
        onRestoreRecurringExpenseClicked = viewModel::onRestoreRecurringExpenseClicked,
        onCheckAllPastEntriesConfirmPressed = viewModel::onCheckAllPastEntriesConfirmPressed,
        onNewBalanceSelected = viewModel::onNewBalanceSelected,
        onAddRecurringEntryPressed = viewModel::onAddRecurringEntryPressed,
        onAddEntryPressed = viewModel::onAddEntryPressed,
        onExpenseCheckedChange = viewModel::onExpenseChecked,
        onExpensePressed = viewModel::onExpensePressed,
        onExpenseLongPressed = viewModel::onExpenseLongPressed,
        onDeleteRecurringExpenseClicked = viewModel::onDeleteRecurringExpenseClicked,
        onDeleteExpenseClicked = viewModel::onDeleteExpenseClicked,
        onEditExpensePressed = viewModel::onEditExpensePressed,
        onEditRecurringExpenseOccurrenceAndFollowingOnesPressed = viewModel::onEditRecurringExpenseOccurenceAndFollowingOnesPressed,
        onEditRecurringExpenseOccurrencePressed = viewModel::onEditRecurringExpenseOccurencePressed,
        navigateToOnboarding = navigateToOnboarding,
        onOnboardingResult = viewModel::onOnboardingResult,
        closeApp = closeApp,
        navigateToPremium = navigateToPremium,
        navigateToMonthlyReport = navigateToMonthlyReport,
        navigateToManageAccount = navigateToManageAccount,
        navigateToSettings = navigateToSettings,
        navigateToLogin = navigateToLogin,
        navigateToCreateAccount = navigateToCreateAccount,
        navigateToAddExpense = navigateToAddExpense,
        navigateToAddRecurringExpense = navigateToAddRecurringExpense,
        onMonthlyReportHintDismissed = viewModel::onMonthlyReportHintDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainView(
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    dbStateFlow: StateFlow<MainViewModel.DBState>,
    eventFlow: Flow<MainViewModel.Event>,
    showMonthlyReportHintFlow: StateFlow<Boolean>,
    openAddExpenseScreenLiveFlow: Flow<Unit>,
    openAddRecurringExpenseScreenLiveFlow: Flow<Unit>,
    openMonthlyReportScreenFromNotificationFlow: Flow<Unit>,
    forceRefreshDataFlow: Flow<Unit>,
    firstDayOfWeekFlow: StateFlow<DayOfWeek>,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
    selectedDateFlow: StateFlow<LocalDate>,
    lowMoneyAmountWarningFlow: StateFlow<Int>,
    goBackToCurrentMonthEventFlow: Flow<Unit>,
    appInitDate: LocalDate,
    showActionButtonsFlow: StateFlow<Boolean>,
    showPremiumRelatedButtonsFlow: StateFlow<Boolean>,
    showManageAccountButtonFlow: StateFlow<Boolean>,
    showGoBackToCurrentMonthButtonFlow: StateFlow<Boolean>,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    userCurrencyFlow: StateFlow<Currency>,
    recurringExpenseDeletionProgressFlow: StateFlow<MainViewModel.RecurringExpenseDeleteProgressState>,
    recurringExpenseRestoreProgressFlow: StateFlow<MainViewModel.RecurringExpenseRestoreProgressState>,
    dayDataFlow: StateFlow<MainViewModel.SelectedDateExpensesData>,
    showExpensesCheckBoxFlow: StateFlow<Boolean>,
    onboardingResultFlow: Flow<OnboardingResult>,
    shouldNavigateToOnboarding: Boolean,
    onSettingsButtonPressed: () -> Unit,
    onAdjustCurrentBalanceButtonPressed: () -> Unit,
    onTickAllPastEntriesButtonPressed: () -> Unit,
    onManageAccountButtonPressed: () -> Unit,
    onDiscoverPremiumButtonPressed: () -> Unit,
    onMonthlyReportButtonPressed: () -> Unit,
    onGoBackToCurrentMonthButtonPressed: () -> Unit,
    onCurrentAccountTapped: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
    onRetryDBLoadingButtonPressed: () -> Unit,
    onAccountSelected: (MainViewModel.SelectedAccount.Selected) -> Unit,
    onExpenseDeletionCancelled: (RestoreAction) -> Unit,
    onCurrentBalanceEditedCancelled: (Expense, Double) -> Unit,
    onRestoreRecurringExpenseClicked: (RecurringExpense, RestoreAction) -> Unit,
    onCheckAllPastEntriesConfirmPressed: () -> Unit,
    onNewBalanceSelected: (Double, String) -> Unit,
    onAddRecurringEntryPressed: () -> Unit,
    onAddEntryPressed: () -> Unit,
    onExpenseCheckedChange: (Expense, Boolean) -> Unit,
    onExpensePressed: (Expense) -> Unit,
    onExpenseLongPressed: (Expense) -> Unit,
    onDeleteRecurringExpenseClicked: (Expense, RecurringExpenseDeleteType) -> Unit,
    onDeleteExpenseClicked: (Expense) -> Unit,
    onEditExpensePressed: (Expense) -> Unit,
    onEditRecurringExpenseOccurrenceAndFollowingOnesPressed: (Expense) -> Unit,
    onEditRecurringExpenseOccurrencePressed: (Expense) -> Unit,
    navigateToOnboarding: () -> Unit,
    onOnboardingResult: (OnboardingResult) -> Unit,
    closeApp: () -> Unit,
    navigateToPremium: (startOnPro: Boolean) -> Unit,
    navigateToMonthlyReport: (fromNotification: Boolean) -> Unit,
    navigateToManageAccount: (MainViewModel.SelectedAccount.Selected.Online) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToLogin: (shouldDismissAfterAuth: Boolean) -> Unit,
    navigateToCreateAccount: () -> Unit,
    navigateToAddExpense: (LocalDate, Expense?) -> Unit,
    navigateToAddRecurringExpense: (LocalDate, Expense?) -> Unit,
    onMonthlyReportHintDismissed: () -> Unit,
) {
    var showAccountSelectorModal by rememberSaveable { mutableStateOf(false) }
    val accountSelectorModalSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFABMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(key1 = "startOnboarding") {
        if (shouldNavigateToOnboarding) {
            navigateToOnboarding()
        }
    }

    LaunchedEffect(key1 = "openAddExpenseScreen") {
        launch {
            dbStateFlow
                .flatMapLatest { state ->
                    if (state is MainViewModel.DBState.Loaded) {
                        return@flatMapLatest openAddExpenseScreenLiveFlow
                    } else {
                        return@flatMapLatest flow {  }
                    }
                }
                .collect {
                    navigateToAddExpense(LocalDate.now(), null)
                }
        }
    }

    LaunchedEffect(key1 = "openAddRecurringExpenseScreen") {
        launch {
            dbStateFlow
                .flatMapLatest { state ->
                    if (state is MainViewModel.DBState.Loaded) {
                        return@flatMapLatest openAddRecurringExpenseScreenLiveFlow
                    } else {
                        return@flatMapLatest flow {  }
                    }
                }
                .collect {
                    navigateToAddRecurringExpense(LocalDate.now(), null)
                }
        }
    }

    LaunchedEffect(key1 = "openMonthlyReportScreen") {
        launch {
            dbStateFlow
                .flatMapLatest { state ->
                    if (state is MainViewModel.DBState.Loaded) {
                        return@flatMapLatest openMonthlyReportScreenFromNotificationFlow
                    } else {
                        return@flatMapLatest flow {  }
                    }
                }
                .collect {
                    navigateToMonthlyReport(true)
                }
        }
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is MainViewModel.Event.CheckAllPastEntriesError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.check_all_past_expences_error_title)
                        .setMessage(
                            context.getString(
                                R.string.check_all_past_expences_error_message,
                                event.error.localizedMessage,
                            )
                        )
                        .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                        .show()
                }
                is MainViewModel.Event.CurrentBalanceEditionError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.adjust_balance_error_title)
                        .setMessage(R.string.adjust_balance_error_message)
                        .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                        .show()
                }
                is MainViewModel.Event.CurrentBalanceEditionSuccess -> {
                    val (expense, diff, newBalance) = event.data

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.adjust_balance_snackbar_text,
                                CurrencyHelper.getFormattedCurrencyString(
                                    currency = userCurrencyFlow.value,
                                    amount = newBalance,
                                )
                            ),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Short,
                        )

                        if (result === SnackbarResult.ActionPerformed) {
                            onCurrentBalanceEditedCancelled(expense, diff)
                        }
                    }
                }
                is MainViewModel.Event.CurrentBalanceRestorationError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.adjust_balance_error_title)
                        .setMessage(R.string.adjust_balance_error_message)
                        .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseCheckingError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.expense_check_error_title)
                        .setMessage(
                            context.getString(
                                R.string.expense_check_error_message,
                                event.error.localizedMessage
                            )
                        )
                        .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseDeletionError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.expense_delete_error_title)
                        .setMessage(R.string.expense_delete_error_message)
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseDeletionSuccess -> {
                    val (deletedExpense, restoreAction) = event.data

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(if (deletedExpense.isRevenue()) R.string.income_delete_snackbar_text else R.string.expense_delete_snackbar_text),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Short,
                        )

                        if (result === SnackbarResult.ActionPerformed) {
                            onExpenseDeletionCancelled(restoreAction)
                        }
                    }
                }
                MainViewModel.Event.GoBackToCurrentMonth -> Unit /* No-op */
                is MainViewModel.Event.OpenAddExpense -> {
                    navigateToAddExpense(event.date, null)
                }
                is MainViewModel.Event.OpenAddRecurringExpense -> {
                    navigateToAddRecurringExpense(event.date, null)
                }
                is MainViewModel.Event.OpenManageAccount -> navigateToManageAccount(event.account)
                MainViewModel.Event.OpenMonthlyReport -> navigateToMonthlyReport(false)
                MainViewModel.Event.OpenPremium -> navigateToPremium(false)
                is MainViewModel.Event.RecurringExpenseDeletionResult -> {
                    when(event.data) {
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorCantDeleteBeforeFirstOccurrence -> {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.recurring_expense_delete_first_error_title)
                                .setMessage(R.string.recurring_expense_delete_first_error_message)
                                .setNegativeButton(R.string.ok, null)
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorIO -> {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.recurring_expense_delete_error_title)
                                .setMessage(R.string.recurring_expense_delete_error_message)
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorRecurringExpenseDeleteNotAssociated -> {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.recurring_expense_delete_error_title)
                                .setMessage(R.string.recurring_expense_delete_error_message)
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.Success -> {
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.recurring_expense_delete_success_message),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )

                                if (result === SnackbarResult.ActionPerformed) {
                                    onRestoreRecurringExpenseClicked(
                                        event.data.recurringExpense,
                                        event.data.restoreAction,
                                    )
                                }
                            }
                        }
                    }
                }
                is MainViewModel.Event.RecurringExpenseRestoreResult -> {
                    when(event.data) {
                        is MainViewModel.RecurringExpenseRestoreEvent.ErrorIO -> {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.recurring_expense_restore_error_title)
                                .setMessage(context.getString(R.string.recurring_expense_restore_error_message))
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseRestoreEvent.Success -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.recurring_expense_restored_success_message),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        }
                    }
                }
                MainViewModel.Event.ShowAccountSelect -> {
                    showAccountSelectorModal = true
                }
                MainViewModel.Event.ShowConfirmCheckAllPastEntries -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.check_all_past_expences_title)
                        .setMessage(R.string.check_all_past_expences_message)
                        .setPositiveButton(R.string.check_all_past_expences_confirm_cta) { dialog2, _ ->
                            onCheckAllPastEntriesConfirmPressed()
                            dialog2.dismiss()
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog2, _ -> dialog2.dismiss() }
                        .show()
                }
                MainViewModel.Event.ShowSettings -> {
                    navigateToSettings()
                }
                is MainViewModel.Event.StartCurrentBalanceEditor -> {
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_adjust_balance, null)
                    val amountEditText = dialogView.findViewById<EditText>(R.id.balance_amount)
                    amountEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    amountEditText.setText(
                        if (event.currentBalance == 0.0) "0" else CurrencyHelper.getFormattedAmountValue(
                            event.currentBalance
                        )
                    )
                    amountEditText.preventUnsupportedInputForDecimals()
                    amountEditText.setSelection(amountEditText.text.length) // Put focus at the end of the text

                    val builder = MaterialAlertDialogBuilder(context)
                    builder.setTitle(R.string.adjust_balance_title)
                    builder.setMessage(R.string.adjust_balance_message)
                    builder.setView(dialogView)
                    builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    builder.setPositiveButton(R.string.ok) { dialog, _ ->
                        try {
                            val stringValue = amountEditText.text.toString()
                            if (stringValue.isNotBlank()) {
                                val newBalance = java.lang.Double.valueOf(stringValue)
                                onNewBalanceSelected(
                                    newBalance,
                                    context.getString(R.string.adjust_balance_expense_title)
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
                        if (hasFocus) {
                            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        }
                    }
                    amountEditText.requestFocus()
                }
                is MainViewModel.Event.ShowExpenseEditionOptions -> {
                    val expense = event.expense
                    if (expense.isRecurring()) {
                        val builder = MaterialAlertDialogBuilder(context)
                        builder.setTitle(if (expense.isRevenue()) R.string.dialog_edit_recurring_income_title else R.string.dialog_edit_recurring_expense_title)
                        builder.setItems(if (expense.isRevenue()) R.array.dialog_edit_recurring_income_choices else R.array.dialog_edit_recurring_expense_choices) { _, which ->
                            when (which) {
                                // Edit this one
                                0 -> onEditRecurringExpenseOccurrencePressed(expense)
                                // Edit this one and following ones
                                1 -> onEditRecurringExpenseOccurrenceAndFollowingOnesPressed(expense)
                                // Delete this one
                                2 -> onDeleteRecurringExpenseClicked(expense, RecurringExpenseDeleteType.ONE)
                                // Delete from
                                3 -> onDeleteRecurringExpenseClicked(expense, RecurringExpenseDeleteType.FROM)
                                // Delete up to
                                4 -> onDeleteRecurringExpenseClicked(expense, RecurringExpenseDeleteType.TO)
                                // Delete all
                                5 -> onDeleteRecurringExpenseClicked(expense, RecurringExpenseDeleteType.ALL)
                            }
                        }
                        builder.show()
                    } else {
                        val builder = MaterialAlertDialogBuilder(context)
                        builder.setTitle(if (expense.isRevenue()) R.string.dialog_edit_income_title else R.string.dialog_edit_expense_title)
                        builder.setItems(if (expense.isRevenue()) R.array.dialog_edit_income_choices else R.array.dialog_edit_expense_choices) { _, which ->
                            when (which) {
                                0 // Edit expense
                                -> onEditExpensePressed(expense)
                                1 // Delete
                                -> onDeleteExpenseClicked(expense)
                            }
                        }
                        builder.show()
                    }
                }
                is MainViewModel.Event.OpenEditExpense -> {
                    navigateToAddExpense(event.expense.date, event.expense)
                }
                is MainViewModel.Event.OpenEditRecurringExpenseOccurrence -> {
                    navigateToAddExpense(event.expense.date, event.expense)
                }
                is MainViewModel.Event.OpenEditRecurringExpenseOccurrenceAndFollowingOnes -> {
                    navigateToAddRecurringExpense(event.expense.date, event.expense)
                }
                MainViewModel.Event.StartOnboarding -> navigateToOnboarding()
                MainViewModel.Event.CloseApp -> closeApp()
            }
        }
    }

    LaunchedEffect(key1 = "recurringExpenseDeletionProgressDialog") {
        var expenseDeletionDialog: ProgressDialog? = null
        launchCollect(recurringExpenseDeletionProgressFlow) { state ->
            when(state) {
                is MainViewModel.RecurringExpenseDeleteProgressState.Deleting -> {
                    val dialog = ProgressDialog(context)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_delete_loading_title)
                    dialog.setMessage(context.getString(R.string.recurring_expense_delete_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseDeletionDialog = dialog
                }
                MainViewModel.RecurringExpenseDeleteProgressState.Idle -> {
                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
            }
        }
    }

    LaunchedEffect(key1 = "expenseRestorationProgressDialog") {
        var expenseRestoreDialog: ProgressDialog? = null
        launchCollect(recurringExpenseRestoreProgressFlow) { state ->
            when(state) {
                is MainViewModel.RecurringExpenseRestoreProgressState.Restoring -> {
                    val dialog = ProgressDialog(context)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_restoring_loading_title)
                    dialog.setMessage(context.getString(R.string.recurring_expense_restoring_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseRestoreDialog = dialog
                }
                MainViewModel.RecurringExpenseRestoreProgressState.Idle -> {
                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
            }
        }
    }

    LaunchedEffect(key1 = "onboardingResultListener") {
        launchCollect(onboardingResultFlow) { result ->
            onOnboardingResult(result)
        }
    }

    Scaffold(
        topBar = {
            MainViewTopBar(
                showActionButtonsFlow = showActionButtonsFlow,
                showPremiumRelatedButtonsFlow = showPremiumRelatedButtonsFlow,
                showManageAccountButtonFlow = showManageAccountButtonFlow,
                showGoBackToCurrentMonthButtonFlow = showGoBackToCurrentMonthButtonFlow,
                onSettingsButtonPressed = onSettingsButtonPressed,
                onAdjustCurrentBalanceButtonPressed = onAdjustCurrentBalanceButtonPressed,
                onTickAllPastEntriesButtonPressed = onTickAllPastEntriesButtonPressed,
                onManageAccountButtonPressed = onManageAccountButtonPressed,
                onDiscoverPremiumButtonPressed = onDiscoverPremiumButtonPressed,
                onMonthlyReportButtonPressed = onMonthlyReportButtonPressed,
                onGoBackToCurrentMonthButtonPressed = onGoBackToCurrentMonthButtonPressed,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    actionColor = colorResource(R.color.snackbar_action_undo),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showFABMenu = !showFABMenu
                },
                containerColor = colorResource(R.color.home_fab_button_color),
                contentColor = colorResource(R.color.white),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = stringResource(R.string.fab_add_expense),
                )
            }
        },
        content = { contentPadding ->
            Box {
                MainViewContent(
                    modifier = Modifier.padding(
                        top = contentPadding.calculateTopPadding(),
                        start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    ),
                    selectedAccountFlow = selectedAccountFlow,
                    dbStateFlow = dbStateFlow,
                    hasPendingInvitationsFlow = hasPendingInvitationsFlow,
                    forceRefreshDataFlow = forceRefreshDataFlow,
                    firstDayOfWeekFlow = firstDayOfWeekFlow,
                    includeCheckedBalanceFlow = includeCheckedBalanceFlow,
                    getDataForMonth = getDataForMonth,
                    selectedDateFlow = selectedDateFlow,
                    lowMoneyAmountWarningFlow = lowMoneyAmountWarningFlow,
                    goBackToCurrentMonthEventFlow = goBackToCurrentMonthEventFlow,
                    dayDataFlow = dayDataFlow,
                    userCurrencyFlow = userCurrencyFlow,
                    showExpensesCheckBoxFlow = showExpensesCheckBoxFlow,
                    appInitDate = appInitDate,
                    onCurrentAccountTapped = onCurrentAccountTapped,
                    onMonthChanged = onMonthChanged,
                    onDateClicked = onDateClicked,
                    onDateLongClicked = onDateLongClicked,
                    onRetryDBLoadingButtonPressed = onRetryDBLoadingButtonPressed,
                    onExpenseCheckedChange = onExpenseCheckedChange,
                    onExpensePressed = onExpensePressed,
                    onExpenseLongPressed = onExpenseLongPressed,
                )

                val showMonthlyReportHint by showMonthlyReportHintFlow.collectAsState()
                if (showMonthlyReportHint) {
                    MonthlyReportHint(
                        modifier = Modifier
                            .padding(contentPadding)
                            .align(Alignment.TopEnd)
                            .offset(x = (-37).dp, y = (-6).dp),
                        onDismiss = onMonthlyReportHintDismissed,
                    )
                }

                if (showAccountSelectorModal) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showAccountSelectorModal = false
                        },
                        sheetState = accountSelectorModalSheetState,
                        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                    ) {
                        AccountSelectorView(
                            onAccountSelected = { account ->
                                onAccountSelected(account)
                                coroutineScope.launch {
                                    accountSelectorModalSheetState.hide()
                                    showAccountSelectorModal = false
                                }
                            },
                            onOpenBecomeProScreen = {
                                navigateToPremium(true)

                                coroutineScope.launch {
                                    accountSelectorModalSheetState.hide()
                                    showAccountSelectorModal = false
                                }
                            },
                            onOpenLoginScreen = { shouldDismissAfterAuth ->
                                navigateToLogin(shouldDismissAfterAuth)
                            },
                            onOpenCreateAccountScreen = {
                                navigateToCreateAccount()
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showFABMenu,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    FABMenuOverlay(
                        onAddRecurringEntryPressed = {
                            onAddRecurringEntryPressed()
                            showFABMenu = false
                        },
                        onAddEntryPressed = {
                            onAddEntryPressed()
                            showFABMenu = false
                        },
                        onTapOutsideCTAs = {
                            showFABMenu = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ProAccountSelectedPreview() {
    Preview(
        dbState = MainViewModel.DBState.Loaded(AppModule.provideDB(LocalContext.current)),
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun DBLoadingPreview() {
    Preview(
        dbState = MainViewModel.DBState.Loading,
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun DBErrorLoadingPreview() {
    Preview(
        dbState = MainViewModel.DBState.Error(RuntimeException("Error")),
    )
}

@Composable
private fun Preview(
    dbState: MainViewModel.DBState,
) {
    AppTheme {
        MainView(
            selectedAccountFlow = MutableStateFlow(MainViewModel.SelectedAccount.Selected.Online(
                name = "Account name",
                isOwner = true,
                ownerEmail = "test@test.com",
                accountId = "accountId",
                accountSecret = "accountSecret",
            )),
            dbStateFlow = MutableStateFlow(dbState),
            eventFlow = MutableSharedFlow(),
            showMonthlyReportHintFlow = MutableStateFlow(false),
            openAddExpenseScreenLiveFlow = MutableSharedFlow(),
            openAddRecurringExpenseScreenLiveFlow = MutableSharedFlow(),
            openMonthlyReportScreenFromNotificationFlow = MutableSharedFlow(),
            forceRefreshDataFlow = MutableSharedFlow(),
            firstDayOfWeekFlow = MutableStateFlow(DayOfWeek.MONDAY),
            includeCheckedBalanceFlow = MutableStateFlow(true),
            getDataForMonth = { yearMonth ->
                DataForMonth(
                    month = yearMonth,
                    daysData = yearMonth.lengthOfMonth().let { days ->
                        (-6..days + 6).associate { day ->
                            val date = yearMonth.atStartOfMonth().plusDays(day.toLong())

                            Pair(
                                date,
                                DataForDay(
                                    day = date,
                                    expenses = emptyList(),
                                    balance = 0.0,
                                    checkedBalance = 0.0,
                                )
                            )
                        }
                    }
                )
            },
            selectedDateFlow = MutableStateFlow(LocalDate.now()),
            lowMoneyAmountWarningFlow = MutableStateFlow(50),
            goBackToCurrentMonthEventFlow = MutableSharedFlow(),
            appInitDate = LocalDate.now(),
            showActionButtonsFlow = MutableStateFlow(true),
            showPremiumRelatedButtonsFlow = MutableStateFlow(true),
            showManageAccountButtonFlow = MutableStateFlow(true),
            showGoBackToCurrentMonthButtonFlow = MutableStateFlow(false),
            hasPendingInvitationsFlow = MutableStateFlow(false),
            userCurrencyFlow = MutableStateFlow(Currency.getInstance("USD")),
            recurringExpenseDeletionProgressFlow = MutableStateFlow(MainViewModel.RecurringExpenseDeleteProgressState.Idle),
            recurringExpenseRestoreProgressFlow = MutableStateFlow(MainViewModel.RecurringExpenseRestoreProgressState.Idle),
            dayDataFlow = MutableStateFlow(MainViewModel.SelectedDateExpensesData.DataAvailable(
                date = LocalDate.now(),
                balance = 100.0,
                checkedBalance = 20.0,
                expenses = listOf(
                    Expense(
                        id = 1L,
                        date = LocalDate.now(),
                        title = "Test",
                        amount = 10.0,
                        checked = false,
                    ),
                    Expense(
                        id = 2L,
                        date = LocalDate.now(),
                        title = "Test 2",
                        amount = -10.0,
                        checked = true,
                        associatedRecurringExpense = AssociatedRecurringExpense(
                            recurringExpense = RecurringExpense(
                                title = "Test",
                                originalAmount = -10.0,
                                recurringDate = LocalDate.now(),
                                type = RecurringExpenseType.WEEKLY,
                            ),
                            originalDate = LocalDate.now(),
                        )
                    )
                ),
            )),
            showExpensesCheckBoxFlow = MutableStateFlow(true),
            onboardingResultFlow = MutableSharedFlow(),
            shouldNavigateToOnboarding = false,
            onSettingsButtonPressed = {},
            onAdjustCurrentBalanceButtonPressed = {},
            onTickAllPastEntriesButtonPressed = {},
            onManageAccountButtonPressed = {},
            onDiscoverPremiumButtonPressed = {},
            onMonthlyReportButtonPressed = {},
            onGoBackToCurrentMonthButtonPressed = {},
            onCurrentAccountTapped = {},
            onMonthChanged = {},
            onDateClicked = {},
            onDateLongClicked = {},
            onRetryDBLoadingButtonPressed = {},
            onAccountSelected = {},
            onExpenseDeletionCancelled = {},
            onCurrentBalanceEditedCancelled = {_, _ ->},
            onRestoreRecurringExpenseClicked = {_, _ ->},
            onCheckAllPastEntriesConfirmPressed = {},
            onNewBalanceSelected = {_, _ ->},
            onAddRecurringEntryPressed = {},
            onAddEntryPressed = {},
            onExpenseCheckedChange = {_, _ ->},
            onExpensePressed = {},
            onExpenseLongPressed = {},
            onDeleteRecurringExpenseClicked = {_, _ ->},
            onDeleteExpenseClicked = {},
            onEditExpensePressed = {},
            onEditRecurringExpenseOccurrenceAndFollowingOnesPressed = {},
            onEditRecurringExpenseOccurrencePressed = {},
            navigateToOnboarding = {},
            onOnboardingResult = {},
            closeApp = {},
            navigateToPremium = {},
            navigateToMonthlyReport = {},
            navigateToManageAccount = {},
            navigateToSettings = {},
            navigateToLogin = {},
            navigateToCreateAccount = {},
            navigateToAddExpense = { _, _ -> },
            navigateToAddRecurringExpense = { _, _ -> },
            onMonthlyReportHintDismissed = {},
        )
    }
}