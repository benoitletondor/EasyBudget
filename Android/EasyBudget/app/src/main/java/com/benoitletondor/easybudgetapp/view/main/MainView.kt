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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
object MainDestination

@Composable
fun MainView(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
) {
    MainView(
        showActionButtonsFlow = viewModel.showMenuActionButtonsFlow,
        showPremiumRelatedButtonsFlow = viewModel.showPremiumRelatedButtonsFlow,
        showManageAccountButtonFlow = viewModel.showManageAccountMenuItemFlow,
        showGoBackToCurrentMonthButtonFlow = viewModel.showGoToCurrentMonthButtonStateFlow,
        selectedAccountFlow = viewModel.accountSelectionFlow,
        hasPendingInvitationsFlow = viewModel.hasPendingInvitationsFlow,
        onSettingsButtonPressed = viewModel::onSettingsButtonPressed,
        onAdjustCurrentBalanceButtonPressed = viewModel::onAdjustCurrentBalanceClicked,
        onTickAllPastEntriesButtonPressed = viewModel::onCheckAllPastEntriesPressed,
        onManageAccountButtonPressed = viewModel::onManageAccountButtonPressed,
        onDiscoverPremiumButtonPressed = viewModel::onDiscoverPremiumButtonPressed,
        onMonthlyReportButtonPressed = viewModel::onMonthlyReportButtonPressed,
        onGoBackToCurrentMonthButtonPressed = viewModel::onGoBackToCurrentMonthButtonPressed,
        onCurrentAccountTapped = viewModel::onCurrentAccountTapped,
    )
}

@Composable
private fun MainView(
    showActionButtonsFlow: StateFlow<Boolean>,
    showPremiumRelatedButtonsFlow: StateFlow<Boolean>,
    showManageAccountButtonFlow: StateFlow<Boolean>,
    showGoBackToCurrentMonthButtonFlow: StateFlow<Boolean>,
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    onSettingsButtonPressed: () -> Unit,
    onAdjustCurrentBalanceButtonPressed: () -> Unit,
    onTickAllPastEntriesButtonPressed: () -> Unit,
    onManageAccountButtonPressed: () -> Unit,
    onDiscoverPremiumButtonPressed: () -> Unit,
    onMonthlyReportButtonPressed: () -> Unit,
    onGoBackToCurrentMonthButtonPressed: () -> Unit,
    onCurrentAccountTapped: () -> Unit,
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
                MainView(
                    selectedAccountFlow = selectedAccountFlow,
                    hasPendingInvitationsFlow = hasPendingInvitationsFlow,
                    onCurrentAccountTapped = onCurrentAccountTapped,
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
private fun MainView(
    selectedAccountFlow: StateFlow<MainViewModel.SelectedAccount>,
    hasPendingInvitationsFlow: StateFlow<Boolean>,
    onCurrentAccountTapped: () -> Unit,
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

                CalendarView(
                    selectedAccount = selectedAccount,
                )

                ExpensesView(
                    selectedAccount = selectedAccount,
                )
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
private fun CalendarView(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
) {

}

@Composable
private fun ExpensesView(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
) {

}

@Composable
@Preview
private fun ProAccountSelectedPreview() {
    AppTheme {
        MainView(
            showActionButtonsFlow = MutableStateFlow(true),
            showPremiumRelatedButtonsFlow = MutableStateFlow(true),
            showManageAccountButtonFlow = MutableStateFlow(true),
            showGoBackToCurrentMonthButtonFlow = MutableStateFlow(false),
            selectedAccountFlow = MutableStateFlow(MainViewModel.SelectedAccount.Selected.Online(
                name = "Account name",
                isOwner = true,
                ownerEmail = "test@test.com",
                accountId = "accountId",
                accountSecret = "accountSecret",
            )),
            hasPendingInvitationsFlow = MutableStateFlow(false),
            onSettingsButtonPressed = {},
            onAdjustCurrentBalanceButtonPressed = {},
            onTickAllPastEntriesButtonPressed = {},
            onManageAccountButtonPressed = {},
            onDiscoverPremiumButtonPressed = {},
            onMonthlyReportButtonPressed = {},
            onGoBackToCurrentMonthButtonPressed = {},
            onCurrentAccountTapped = {},
        )
    }
}