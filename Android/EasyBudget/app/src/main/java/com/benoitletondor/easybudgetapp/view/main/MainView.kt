package com.benoitletondor.easybudgetapp.view.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.compose.AppTopAppBar
import com.benoitletondor.easybudgetapp.compose.AppTopBarMoreMenuItem
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.preventUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.accountselector.AccountSelectorView
import com.benoitletondor.easybudgetapp.view.main.calendar.CalendarView
import com.benoitletondor.easybudgetapp.view.main.createaccount.CreateAccountActivity
import com.benoitletondor.easybudgetapp.view.main.login.LoginActivity
import com.benoitletondor.easybudgetapp.view.main.manageaccount.ManageAccountActivity
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kizitonwose.calendar.core.atStartOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
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
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(),
) {
    MainView(
        selectedAccountFlow = viewModel.accountSelectionFlow,
        dbStateFlow = viewModel.dbAvailableFlow,
        eventFlow = viewModel.eventFlow,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainView(
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    dbStateFlow: StateFlow<MainViewModel.DBState>,
    eventFlow: Flow<MainViewModel.Event>,
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
) {
    var showAccountSelectorModal by remember { mutableStateOf(false) }
    val accountSelectorModalSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFABMenu by remember { mutableStateOf(false) }

    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is MainViewModel.Event.CheckAllPastEntriesError -> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.check_all_past_expences_error_title)
                        .setMessage(
                            activity.getString(
                                R.string.check_all_past_expences_error_message,
                                event.error.localizedMessage,
                            )
                        )
                        .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                        .show()
                }
                is MainViewModel.Event.CurrentBalanceEditionError -> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.adjust_balance_error_title)
                        .setMessage(R.string.adjust_balance_error_message)
                        .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                        .show()
                }
                is MainViewModel.Event.CurrentBalanceEditionSuccess -> {
                    val (expense, diff, newBalance) = event.data

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = activity.getString(
                                R.string.adjust_balance_snackbar_text,
                                CurrencyHelper.getFormattedCurrencyString(
                                    currency = userCurrencyFlow.value,
                                    amount = newBalance,
                                )
                            ),
                            actionLabel = activity.getString(R.string.undo),
                            duration = SnackbarDuration.Long,
                        )

                        if (result === SnackbarResult.ActionPerformed) {
                            onCurrentBalanceEditedCancelled(expense, diff)
                        }
                    }
                }
                is MainViewModel.Event.CurrentBalanceRestorationError -> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.adjust_balance_error_title)
                        .setMessage(R.string.adjust_balance_error_message)
                        .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseCheckingError -> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.expense_check_error_title)
                        .setMessage(
                            activity.getString(
                                R.string.expense_check_error_message,
                                event.error.localizedMessage
                            )
                        )
                        .setNegativeButton(R.string.ok) { dialog2, _ -> dialog2.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseDeletionError -> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.expense_delete_error_title)
                        .setMessage(R.string.expense_delete_error_message)
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                is MainViewModel.Event.ExpenseDeletionSuccess -> {
                    val (deletedExpense, restoreAction) = event.data

                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = activity.getString(if (deletedExpense.isRevenue()) R.string.income_delete_snackbar_text else R.string.expense_delete_snackbar_text),
                            actionLabel = activity.getString(R.string.undo),
                            duration = SnackbarDuration.Long,
                        )

                        if (result === SnackbarResult.ActionPerformed) {
                            onExpenseDeletionCancelled(restoreAction)
                        }
                    }
                }
                MainViewModel.Event.GoBackToCurrentMonth -> Unit /* No-op */
                is MainViewModel.Event.OpenAddExpense -> {
                    // FIXME replace this
                    val startIntent = ExpenseEditActivity.newIntent(
                        context = activity,
                        editedExpense = null,
                        date = event.date,
                    )

                    activity.startActivity(startIntent)
                }
                is MainViewModel.Event.OpenAddRecurringExpense -> {
                    // FIXME replace this
                    val startIntent = RecurringExpenseEditActivity.newIntent(
                        context = activity,
                        editedExpense = null,
                        startDate = event.date,
                    )

                    activity.startActivity(startIntent)
                }
                is MainViewModel.Event.OpenManageAccount -> {
                    // FIXME replace this
                    activity.startActivity(ManageAccountActivity.newIntent(activity, event.account))
                }
                MainViewModel.Event.OpenMonthlyReport -> {
                    // FIXME replace this
                    val startIntent = Intent(activity, MonthlyReportBaseActivity::class.java)
                    activity.startActivity(startIntent)
                }
                MainViewModel.Event.OpenPremium -> {
                    // FIXME replace this
                    val startIntent = Intent(activity, SettingsActivity::class.java)
                    startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)
                    activity.startActivity(startIntent)
                }
                is MainViewModel.Event.RecurringExpenseDeletionResult -> {
                    when(event.data) {
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorCantDeleteBeforeFirstOccurrence -> {
                            MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.recurring_expense_delete_first_error_title)
                                .setMessage(R.string.recurring_expense_delete_first_error_message)
                                .setNegativeButton(R.string.ok, null)
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorIO -> {
                            MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.recurring_expense_delete_error_title)
                                .setMessage(R.string.recurring_expense_delete_error_message)
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.ErrorRecurringExpenseDeleteNotAssociated -> {
                            MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.recurring_expense_delete_error_title)
                                .setMessage(R.string.recurring_expense_delete_error_message)
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseDeletionEvent.Success -> {
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = activity.getString(R.string.recurring_expense_delete_success_message),
                                    actionLabel = activity.getString(R.string.undo),
                                    duration = SnackbarDuration.Long,
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
                            MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.recurring_expense_restore_error_title)
                                .setMessage(activity.getString(R.string.recurring_expense_restore_error_message))
                                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        is MainViewModel.RecurringExpenseRestoreEvent.Success -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = activity.getString(R.string.recurring_expense_restored_success_message),
                                    actionLabel = activity.getString(R.string.undo),
                                    duration = SnackbarDuration.Long,
                                )
                            }
                        }
                    }
                }
                MainViewModel.Event.ShowAccountSelect -> {
                    showAccountSelectorModal = true
                }
                MainViewModel.Event.ShowConfirmCheckAllPastEntries -> {
                    MaterialAlertDialogBuilder(activity)
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
                    // FIXME replace this
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                }
                is MainViewModel.Event.StartCurrentBalanceEditor -> {
                    val dialogView = activity.layoutInflater.inflate(R.layout.dialog_adjust_balance, null)
                    val amountEditText = dialogView.findViewById<EditText>(R.id.balance_amount)
                    amountEditText.setText(
                        if (event.currentBalance == 0.0) "0" else CurrencyHelper.getFormattedAmountValue(
                            event.currentBalance
                        )
                    )
                    amountEditText.preventUnsupportedInputForDecimals()
                    amountEditText.setSelection(amountEditText.text.length) // Put focus at the end of the text

                    val builder = MaterialAlertDialogBuilder(activity)
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
                                    activity.getString(R.string.adjust_balance_expense_title)
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
                        if (hasFocus && activity.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = "recurringExpenseDeletionProgressDialog") {
        var expenseDeletionDialog: ProgressDialog? = null
        launchCollect(recurringExpenseDeletionProgressFlow) { state ->
            when(state) {
                is MainViewModel.RecurringExpenseDeleteProgressState.Deleting -> {
                    val dialog = ProgressDialog(activity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_delete_loading_title)
                    dialog.setMessage(activity.getString(R.string.recurring_expense_delete_loading_message))
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
                    val dialog = ProgressDialog(activity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_restoring_loading_title)
                    dialog.setMessage(activity.getString(R.string.recurring_expense_restoring_loading_message))
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

    Scaffold(
        topBar = {
            MainViewTopAppBar(
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
            SnackbarHost(hostState = snackbarHostState)
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
            Box(
                modifier = Modifier.padding(contentPadding),
            ) {
                MainViewContent(
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
                    appInitDate = appInitDate,
                    onCurrentAccountTapped = onCurrentAccountTapped,
                    onMonthChanged = onMonthChanged,
                    onDateClicked = onDateClicked,
                    onDateLongClicked = onDateLongClicked,
                    onRetryDBLoadingButtonPressed = onRetryDBLoadingButtonPressed,
                )

                if (showAccountSelectorModal) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showAccountSelectorModal = false
                        },
                        sheetState = accountSelectorModalSheetState,
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
                                // FIXME replace this
                                val startIntent = Intent(activity, SettingsActivity::class.java)
                                startIntent.putExtra(SettingsActivity.SHOW_PRO_INTENT_KEY, true)
                                activity.startActivity(startIntent)

                                coroutineScope.launch {
                                    accountSelectorModalSheetState.hide()
                                    showAccountSelectorModal = false
                                }
                            },
                            onOpenLoginScreen = { shouldDismissAfterAuth ->
                                // FIXME replace this
                                activity.startActivity(LoginActivity.newIntent(activity, shouldDismissAfterAuth = shouldDismissAfterAuth))
                            },
                            onOpenCreateAccountScreen = {
                                // FIXME replace this
                                activity.startActivity(Intent(activity, CreateAccountActivity::class.java))
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showFABMenu,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = colorResource(R.color.menu_background_overlay_color))
                            .padding(bottom = 90.dp, end = 16.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    showFABMenu = false
                                    onAddRecurringEntryPressed()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color = Color.Black)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                text = stringResource(R.string.fab_add_monthly_expense),
                                color = Color.White,
                                fontSize = 15.sp,
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FloatingActionButton(
                                onClick = {
                                    showFABMenu = false
                                    onAddRecurringEntryPressed()
                                },
                                containerColor = colorResource(R.color.fab_add_monthly_expense),
                                contentColor = colorResource(R.color.white),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_autorenew_white),
                                    contentDescription = stringResource(R.string.fab_add_monthly_expense),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .clickable {
                                    showFABMenu = false
                                    onAddEntryPressed()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color = Color.Black)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                text = stringResource(R.string.fab_add_expense),
                                color = Color.White,
                                fontSize = 15.sp,
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FloatingActionButton(
                                onClick = {
                                    showFABMenu = false
                                    onAddEntryPressed()
                                },
                                containerColor = colorResource(R.color.fab_add_expense),
                                contentColor = colorResource(R.color.white),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_add_24),
                                    contentDescription = stringResource(R.string.fab_add_expense),
                                )
                            }
                        }
                    }

                }
            }
        }
    )
}

@Composable
private fun MainViewTopAppBar(
    showActionButtonsFlow: StateFlow<Boolean>,
    showPremiumRelatedButtonsFlow: StateFlow<Boolean>,
    showManageAccountButtonFlow: StateFlow<Boolean>,
    showGoBackToCurrentMonthButtonFlow: StateFlow<Boolean>,
    onSettingsButtonPressed: () -> Unit,
    onAdjustCurrentBalanceButtonPressed: () -> Unit,
    onTickAllPastEntriesButtonPressed: () -> Unit,
    onManageAccountButtonPressed: () -> Unit,
    onDiscoverPremiumButtonPressed: () -> Unit,
    onMonthlyReportButtonPressed: () -> Unit,
    onGoBackToCurrentMonthButtonPressed: () -> Unit,
) {
    AppTopAppBar(
        title = stringResource(R.string.app_name),
        backButtonBehavior = BackButtonBehavior.Hidden,
        actions = {
            val showActionButtons by showActionButtonsFlow.collectAsState()
            val showPremiumRelatedButtons by showPremiumRelatedButtonsFlow.collectAsState()
            val showManageAccountButton by showManageAccountButtonFlow.collectAsState()
            val showGoBackToCurrentMonthButton by showGoBackToCurrentMonthButtonFlow.collectAsState()

            if (showActionButtons) {
                if (showManageAccountButton) {
                    IconButton(
                        onClick = onManageAccountButtonPressed,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_manage_accounts_24),
                            contentDescription = stringResource(R.string.action_manage_account),
                        )
                    }
                }

                if (showGoBackToCurrentMonthButton) {
                    IconButton(
                        onClick = onGoBackToCurrentMonthButtonPressed,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_today),
                            contentDescription = stringResource(R.string.action_go_to_current_month),
                        )
                    }
                }

                if (showPremiumRelatedButtons) {
                    IconButton(
                        onClick = onMonthlyReportButtonPressed,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_list_alt_24),
                            contentDescription = stringResource(R.string.monthly_report_button_title),
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDiscoverPremiumButtonPressed,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_star_24),
                            contentDescription = stringResource(R.string.action_become_premium),
                        )
                    }
                }
            }

            AppTopBarMoreMenuItem { dismiss ->
                if (showActionButtons) {
                    DropdownMenuItem(
                        onClick = {
                            onAdjustCurrentBalanceButtonPressed()
                            dismiss()
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.action_balance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                    )
                }

                if (showActionButtons && showPremiumRelatedButtons) {
                    DropdownMenuItem(
                        onClick = {
                            onTickAllPastEntriesButtonPressed()
                            dismiss()
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.action_mark_all_past_entries_as_checked),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                    )
                }

                DropdownMenuItem(
                    onClick = {
                        onSettingsButtonPressed()
                        dismiss()
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.action_settings),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                )
            }
        }
    )
}

@Composable
private fun MainViewContent(
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    dbStateFlow: StateFlow<MainViewModel.DBState>,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    forceRefreshDataFlow: Flow<Unit>,
    firstDayOfWeekFlow: StateFlow<DayOfWeek>,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
    selectedDateFlow: StateFlow<LocalDate>,
    lowMoneyAmountWarningFlow: StateFlow<Int>,
    goBackToCurrentMonthEventFlow: Flow<Unit>,
    appInitDate: LocalDate,
    onCurrentAccountTapped: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
    onRetryDBLoadingButtonPressed: () -> Unit,
) {
    val account by selectedAccountFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        when(val selectedAccount = account) {
            MainViewModel.SelectedAccount.Loading -> LoadingView()
            is MainViewModel.SelectedAccount.Selected -> {
                SelectedAccountHeader(
                    selectedAccount = selectedAccount,
                    hasPendingInvitationsFlow = hasPendingInvitationsFlow,
                    onCurrentAccountTapped = onCurrentAccountTapped,
                )

                val dbState by dbStateFlow.collectAsState()

                when(val currentDbState = dbState) {
                    is MainViewModel.DBState.Error -> DBLoadingErrorView(
                        error = currentDbState.error,
                        onRetryButtonClicked = onRetryDBLoadingButtonPressed,
                    )
                    is MainViewModel.DBState.Loaded -> {
                        CalendarView(
                            appInitDate = appInitDate,
                            forceRefreshDataFlow = forceRefreshDataFlow,
                            firstDayOfWeekFlow = firstDayOfWeekFlow,
                            includeCheckedBalanceFlow = includeCheckedBalanceFlow,
                            getDataForMonth = getDataForMonth,
                            selectedDateFlow = selectedDateFlow,
                            lowMoneyAmountWarningFlow = lowMoneyAmountWarningFlow,
                            onMonthChanged = onMonthChanged,
                            goBackToCurrentMonthEventFlow = goBackToCurrentMonthEventFlow,
                            onDateSelected = onDateClicked,
                            onDateLongClicked = onDateLongClicked,
                        )

                        ExpensesView(
                            selectedAccount = selectedAccount,
                        )
                    }
                    MainViewModel.DBState.Loading,
                    MainViewModel.DBState.NotLoaded -> LoadingView()
                }
            }
        }
    }
}

@Composable
private fun SelectedAccountHeader(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    onCurrentAccountTapped: () -> Unit,
) {
    val hasPendingInvitations by hasPendingInvitationsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.status_bar_color))
            .padding(bottom = 8.dp)
            .clickable(onClick = onCurrentAccountTapped)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
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
                        text = when(selectedAccount) {
                            MainViewModel.SelectedAccount.Selected.Offline -> stringResource(R.string.main_account_default_name)
                            is MainViewModel.SelectedAccount.Selected.Online -> stringResource(R.string.main_account_online_name, selectedAccount.name)
                        },
                        maxLines = 1,
                        color = colorResource(R.color.action_bar_text_color),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (hasPendingInvitations) {
                Box(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                ){
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_notifications_24),
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
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_drop_down_24),
                    colorFilter = ColorFilter.tint(colorResource(R.color.action_bar_text_color)),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

    }
}

@Composable
private fun ExpensesView(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
) {

}

@Composable
private fun DBLoadingErrorView(
    error: Throwable,
    onRetryButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.calendar_month_loading_error_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.account_error_loading_message, error.localizedMessage ?: "No error message"),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onRetryButtonClicked,
        ) {
            Text(stringResource(R.string.manage_account_error_cta))
        }
    }
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
        )
    }
}