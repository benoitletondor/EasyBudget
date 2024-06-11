package com.benoitletondor.easybudgetapp.view.main.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.subviews.calendar.CalendarView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency

@Composable
fun MainViewContent(
    modifier: Modifier = Modifier,
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
    dayDataFlow: StateFlow<MainViewModel.SelectedDateExpensesData>,
    userCurrencyFlow: StateFlow<Currency>,
    showExpensesCheckBoxFlow: StateFlow<Boolean>,
    appInitDate: LocalDate,
    onCurrentAccountTapped: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
    onRetryDBLoadingButtonPressed: () -> Unit,
    onExpenseCheckedChange: (Expense, Boolean) -> Unit,
    onExpensePressed: (Expense) -> Unit,
    onExpenseLongPressed: (Expense) -> Unit,
) {
    val account by selectedAccountFlow.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
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
                            dbStateFlow = dbStateFlow,
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
                            dayDataFlow = dayDataFlow,
                            lowMoneyAmountWarningFlow = lowMoneyAmountWarningFlow,
                            userCurrencyFlow = userCurrencyFlow,
                            showExpensesCheckBoxFlow = showExpensesCheckBoxFlow,
                            onExpenseCheckedChange = onExpenseCheckedChange,
                            onExpensePressed = onExpensePressed,
                            onExpenseLongPressed = onExpenseLongPressed,
                        )
                    }
                    MainViewModel.DBState.Loading,
                    MainViewModel.DBState.NotLoaded -> LoadingView()
                }
            }
        }
    }
}