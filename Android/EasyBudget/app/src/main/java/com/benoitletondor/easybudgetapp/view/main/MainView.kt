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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.ui.components.AppTopBarMenuItem
import com.benoitletondor.easybudgetapp.ui.components.AppTopBarMoreMenuItem
import com.benoitletondor.easybudgetapp.ui.components.AppTopBarScaffold
import com.benoitletondor.easybudgetapp.ui.components.LoadingView
import com.benoitletondor.easybudgetapp.view.main.account.AccountView
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun MainView(
    navController: NavController,
    viewModel: MainViewModel,
) {
    // Used to pass data from AccountViewModel and there
    val showGoToCurrentMonthStateFlow = remember { MutableStateFlow(false) }

    AppTopBarScaffold(
        navController = navController,
        showBackButton = false,
        title = stringResource(id = R.string.app_name),
        actions = {
            val iabStatus by viewModel.premiumStatusFlow.collectAsState()
            val selectedAccount by viewModel.accountSelectionFlow.collectAsState()
            val showGoToCurrentMonth by showGoToCurrentMonthStateFlow.collectAsState()

            TopBarActions(
                iabStatus = iabStatus,
                selectedAccount = selectedAccount,
                showGoToCurrentMonth = showGoToCurrentMonth,
            )
        },
    ) {
        val selectedAccount by viewModel.accountSelectionFlow.collectAsState()

        when(val account = selectedAccount) {
            MainViewModel.SelectedAccount.Loading -> {
                LoadingView()
            }
            is MainViewModel.SelectedAccount.Selected -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val hasPendingInvitations by viewModel.hasPendingInvitationsFlow.collectAsState()

                        AccountSelector(
                            selectedAccount = account,
                            hasPendingInvitations = hasPendingInvitations,
                            onAccountTapped = viewModel::onAccountTapped,
                        )
                    }

                    AccountView(
                        viewModel = hiltViewModel(
                            creationCallback = { factory: AccountViewModel.AccountViewModelFactory ->
                                factory.create(
                                    account = account,
                                    showGoToCurrentMonthStateFlow = showGoToCurrentMonthStateFlow,
                                )
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarActions(
    iabStatus: PremiumCheckStatus,
    selectedAccount: MainViewModel.SelectedAccount,
    showGoToCurrentMonth: Boolean,
) {
    val shouldShowManageAccount = selectedAccount is MainViewModel.SelectedAccount.Selected.Online
    if (shouldShowManageAccount) {
        AppTopBarMenuItem(
            icon = painterResource(id = R.drawable.ic_baseline_manage_accounts_24),
            contentDescription = stringResource(R.string.action_manage_account),
            onClick = { /* TODO */ },
        )
    }

    if (showGoToCurrentMonth) {
        AppTopBarMenuItem(
            icon = painterResource(id = R.drawable.ic_list_alt_24),
            contentDescription = stringResource(R.string.action_go_to_current_month),
            onClick = { /* TODO */ },
        )
    }

    val shouldShowBecomePremiumAction = when(iabStatus) {
        PremiumCheckStatus.INITIALIZING,
        PremiumCheckStatus.CHECKING -> false
        PremiumCheckStatus.ERROR,
        PremiumCheckStatus.NOT_PREMIUM -> true
        PremiumCheckStatus.LEGACY_PREMIUM,
        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
        PremiumCheckStatus.PRO_SUBSCRIBED -> false
    }

    val shouldShowMonthlyRecapButton = !shouldShowBecomePremiumAction && selectedAccount is MainViewModel.SelectedAccount.Selected

    if (shouldShowBecomePremiumAction) {
        AppTopBarMenuItem(
            icon = painterResource(id = R.drawable.ic_baseline_star_24),
            contentDescription = stringResource(R.string.action_become_premium),
            onClick = { /* TODO */ },
        )
    }

    if (shouldShowMonthlyRecapButton) {
        AppTopBarMenuItem(
            icon = painterResource(id = R.drawable.ic_list_alt_24),
            contentDescription = stringResource(R.string.monthly_report_button_title),
            onClick = { /* TODO */ },
        )
    }

    AppTopBarMoreMenuItem {
        val shouldShowAdjustCurrentBalance = selectedAccount is MainViewModel.SelectedAccount.Selected
        if (shouldShowAdjustCurrentBalance) {
            DropdownMenuItem(
                onClick = { /* TODO */ },
                text = {
                    Text(
                        text = stringResource(R.string.action_balance),
                        fontSize = 16.sp,
                    )
                },
            )
        }

        val shouldShowTickPastEntries = selectedAccount is MainViewModel.SelectedAccount.Selected && when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING,
            PremiumCheckStatus.ERROR,
            PremiumCheckStatus.NOT_PREMIUM -> false
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.PRO_SUBSCRIBED -> true
        }

        if (shouldShowTickPastEntries) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.action_mark_all_past_entries_as_checked),
                        fontSize = 16.sp,
                    )
                },
                onClick = { /*TODO*/ }
            )
        }

        DropdownMenuItem(
            onClick = { /* TODO */ },
            text = {
                Text(
                    text = stringResource(R.string.action_settings),
                    fontSize = 16.sp,
                )
            },
        )
    }
}

@Composable
private fun AccountSelector(
    selectedAccount: MainViewModel.SelectedAccount.Selected,
    hasPendingInvitations: Boolean,
    onAccountTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.status_bar_color))
            .padding(bottom = 8.dp)
            .clickable(
                onClick = onAccountTapped,
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