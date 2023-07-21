package com.benoitletondor.easybudgetapp.view.main.accountselector.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.accountselector.AccountSelectorViewModel

@Composable
fun AccountsView(viewModel: AccountSelectorViewModel) {
    val state: AccountSelectorViewModel.State by viewModel.stateFlow.collectAsState()

    AccountsView(
        state = state,
        onIabErrorRetryButtonClicked = viewModel::onIabErrorRetryButtonClicked,
        onAccountSelected = viewModel::onAccountSelected,
        onBecomeProButtonClicked = viewModel::onBecomeProButtonClicked,
    )
}

@Composable
private fun AccountsView(
    state: AccountSelectorViewModel.State,
    onIabErrorRetryButtonClicked: () -> Unit,
    onAccountSelected: (MainViewModel.SelectedAccount.Selected) -> Unit,
    onBecomeProButtonClicked: () -> Unit,
) {
    val isLoading = state is AccountSelectorViewModel.State.Loading
    val offlineAccountSelected = when(state) {
        is AccountSelectorViewModel.State.AccountsAvailable -> state.isOfflineSelected
        AccountSelectorViewModel.State.Loading -> false
        AccountSelectorViewModel.State.NotAuthenticated,
        AccountSelectorViewModel.State.IabError,
        AccountSelectorViewModel.State.NotPro -> true
    }

    Column(
        modifier = Modifier
            .padding(
                vertical = 20.dp,
                horizontal = 16.dp
            ),
    ) {
        Text(
            text = "Accounts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Offline",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .apply {
                   if (!isLoading) {
                       clickable {
                           onAccountSelected(MainViewModel.SelectedAccount.Selected.Offline)
                       }
                   }
                },
            tonalElevation = if (isLoading) { 0.dp } else { 2.dp },
            shadowElevation = if (isLoading) { 0.dp } else { 2.dp },
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = offlineAccountSelected,
                    onClick = {},
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Default",
                    fontSize = 16.sp,
                )
            }

        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Online",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when(state) {
            AccountSelectorViewModel.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is AccountSelectorViewModel.State.AccountsAvailable -> OnlineAccounts(
                ownAccounts = state.ownAccounts,
                invitedAccounts = state.invitedAccounts,
            )
            AccountSelectorViewModel.State.IabError -> IabErrorView(
                onRetryButtonClicked = onIabErrorRetryButtonClicked,
            )
            AccountSelectorViewModel.State.NotAuthenticated -> TODO()
            AccountSelectorViewModel.State.NotPro -> NotProView(
                onBecomeProButtonClicked = onBecomeProButtonClicked,
            )
        }
    }
}

@Composable
private fun ColumnScope.OnlineAccounts(
    ownAccounts: List<AccountSelectorViewModel.Account>,
    invitedAccounts: List<AccountSelectorViewModel.Account>,
) {
    TODO()
}

@Composable
private fun ColumnScope.NotProView(
    onBecomeProButtonClicked: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "Want to unlock more accounts?",
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "Add online accounts, always in sync and sharable in real-time with your friends/family/coworkers, by upgrading to Pro.",
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = onBecomeProButtonClicked,
    ) {
        Text("Discover EasyBudget Pro")
    }
}

@Composable
private fun ColumnScope.IabErrorView(
    onRetryButtonClicked: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "Unable to connect to the PlayStore",
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "An error occurred while checking your membership status, please try again and try to restart your phone if the error persists.",
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = onRetryButtonClicked,
    ) {
        Text("Retry")
    }
}

@Composable
@Preview(name = "Loading preview")
fun AccountsLoadingViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.Loading,
            onIabErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
        )
    }
}

@Composable
@Preview(name = "IAB error preview")
fun AccountsIabErrorViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.IabError,
            onIabErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
        )
    }
}

@Composable
@Preview(name = "Not pro preview")
fun AccountsNotProViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.NotPro,
            onIabErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
        )
    }
}

@Composable
@Preview(name = "Not pro preview")
fun AccountsAvailableViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.AccountsAvailable(
                isOfflineSelected = false,
                ownAccounts = listOf(
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = true,
                        name = "Own account 1",
                        ownerEmail = "",
                    ),
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = false,
                        name = "Own account 2 with a super long name to test how it looks and see how the cell behaves",
                        ownerEmail = "",
                    )
                ),
                invitedAccounts = listOf(
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = false,
                        name = "Other person account",
                        ownerEmail = "other.person@gmail.com",
                    ),
                )
            ),
            onIabErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
        )
    }
}