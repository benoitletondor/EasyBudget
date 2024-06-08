package com.benoitletondor.easybudgetapp.view.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.compose.AppTopAppBar
import com.benoitletondor.easybudgetapp.compose.AppTopBarMoreMenuItem
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.benoitletondor.easybudgetapp.view.main.calendar.CalendarView
import com.kizitonwose.calendar.core.atStartOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Serializable
object MainDestination

@Composable
fun MainView(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
) {
    MainView(
        selectedAccountFlow = viewModel.accountSelectionFlow,
        dbStateFlow = viewModel.dbAvailableFlow,
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
    )
}

@Composable
private fun MainView(
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    dbStateFlow: StateFlow<MainViewModel.DBState>,
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
) {
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
                )
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

            AppTopBarMoreMenuItem {
                if (showActionButtons) {
                    DropdownMenuItem(
                        onClick = onAdjustCurrentBalanceButtonPressed,
                        text = {
                            Text(stringResource(R.string.action_balance))
                        },
                    )
                }

                if (showActionButtons && showPremiumRelatedButtons) {
                    DropdownMenuItem(
                        onClick = onTickAllPastEntriesButtonPressed,
                        text = {
                            Text(stringResource(R.string.action_mark_all_past_entries_as_checked))
                        },
                    )
                }

                DropdownMenuItem(
                    onClick = onSettingsButtonPressed,
                    text = {
                        Text(stringResource(R.string.action_settings))
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

                when(dbState) {
                    is MainViewModel.DBState.Error -> TODO()
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
@Preview
private fun ProAccountSelectedPreview() {
    val context = LocalContext.current

    AppTheme {
        MainView(
            selectedAccountFlow = MutableStateFlow(MainViewModel.SelectedAccount.Selected.Online(
                name = "Account name",
                isOwner = true,
                ownerEmail = "test@test.com",
                accountId = "accountId",
                accountSecret = "accountSecret",
            )),
            dbStateFlow = MutableStateFlow(MainViewModel.DBState.Loaded(AppModule.provideDB(context))),
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
        )
    }
}