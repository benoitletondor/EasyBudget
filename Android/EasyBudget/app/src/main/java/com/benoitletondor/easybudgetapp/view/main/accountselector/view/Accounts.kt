package com.benoitletondor.easybudgetapp.view.main.accountselector.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.accountselector.AccountSelectorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun AccountsView(viewModel: AccountSelectorViewModel) {
    val state: AccountSelectorViewModel.State by viewModel.stateFlow.collectAsState()

    AccountsView(
        state = state,
        onIabErrorRetryButtonClicked = viewModel::onIabErrorRetryButtonClicked,
        onErrorRetryButtonClicked = viewModel::onRetryErrorButtonClicked,
        onAccountSelected = viewModel::onAccountSelected,
        onBecomeProButtonClicked = viewModel::onBecomeProButtonClicked,
        onLoginButtonPressed = viewModel::onLoginButtonPressed,
        onCreateAccountClicked = viewModel::onCreateAccountClicked,
        onAcceptInvitationConfirmed = viewModel::onAcceptInvitationConfirmed,
        onRejectInvitationConfirmed = viewModel::onRejectInvitationConfirmed,
    )
}

@Composable
private fun AccountsView(
    state: AccountSelectorViewModel.State,
    onIabErrorRetryButtonClicked: () -> Unit,
    onErrorRetryButtonClicked: () -> Unit,
    onAccountSelected: (MainViewModel.SelectedAccount.Selected) -> Unit,
    onBecomeProButtonClicked: () -> Unit,
    onLoginButtonPressed: () -> Unit,
    onCreateAccountClicked: () -> Unit,
    onAcceptInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
    onRejectInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
) {
    val isLoading = state is AccountSelectorViewModel.State.Loading
    val offlineAccountSelected = when(state) {
        is AccountSelectorViewModel.State.AccountsAvailable -> state.isOfflineSelected
        AccountSelectorViewModel.State.Loading -> false
        is AccountSelectorViewModel.State.NotAuthenticated,
        AccountSelectorViewModel.State.IabError,
        is AccountSelectorViewModel.State.NotPro,
        is AccountSelectorViewModel.State.Error -> true
    }
    val shouldDisplayOfflineBackupEnabled = state is AccountSelectorViewModel.OfflineBackStateAvailable && state.isOfflineBackupEnabled

    Column(
        modifier = Modifier
            .padding(
                vertical = 20.dp,
                horizontal = 16.dp
            )
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Accounts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(26.dp))

        AccountButton(
            title = "Default (Offline)",
            subtitle = null,
            enabled = !isLoading,
            selected = offlineAccountSelected,
            onClick = { onAccountSelected(MainViewModel.SelectedAccount.Selected.Offline) }
        )

        if (shouldDisplayOfflineBackupEnabled) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Automatic cloud backup enabled.",
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = colorResource(R.color.secondary_text),
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Online",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

        if (state is AccountSelectorViewModel.State.AccountsAvailable){
            Text(
                text = state.userEmail,
                fontSize = 15.sp,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when(state) {
            AccountSelectorViewModel.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is AccountSelectorViewModel.State.AccountsAvailable -> OnlineAccountsView(
                ownAccounts = state.ownAccounts,
                showCreateOnlineAccountButton = state.showCreateOnlineAccountButton,
                invitedAccounts = state.invitedAccounts,
                onAccountSelected = {
                    onAccountSelected(MainViewModel.SelectedAccount.Selected.Online(
                        name = it.name,
                        isOwner = it.ownerEmail == state.userEmail,
                        ownerEmail = it.ownerEmail,
                        accountId = it.id,
                        accountSecret = it.secret,
                    ))
                },
                onCreateAccountClicked = onCreateAccountClicked,
                pendingInvitations = state.pendingInvitations,
                onAcceptInvitationConfirmed = onAcceptInvitationConfirmed,
                onRejectInvitationConfirmed = onRejectInvitationConfirmed,
            )
            AccountSelectorViewModel.State.IabError -> IabErrorView(
                onRetryButtonClicked = onIabErrorRetryButtonClicked,
            )
            is AccountSelectorViewModel.State.NotAuthenticated -> NotAuthenticatedView(
                onLoginButtonPressed = onLoginButtonPressed,
            )
            is AccountSelectorViewModel.State.NotPro -> NotProView(
                onBecomeProButtonClicked = onBecomeProButtonClicked,
            )
            is AccountSelectorViewModel.State.Error -> ErrorView(
                error = state.cause,
                onRetryButtonClicked = onErrorRetryButtonClicked,
            )
        }
    }
}

@Composable
private fun ColumnScope.NotAuthenticatedView(
    onLoginButtonPressed: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "Login with your Google account",
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "To create or retrieve your online accounts, login with your Google account.",
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = onLoginButtonPressed,
    ) {
        Text("Login with Google")
    }
}

@Composable
private fun ColumnScope.OnlineAccountsView(
    ownAccounts: List<AccountSelectorViewModel.Account>,
    showCreateOnlineAccountButton: Boolean,
    invitedAccounts: List<AccountSelectorViewModel.Account>,
    onAccountSelected: (AccountSelectorViewModel.Account) -> Unit,
    onCreateAccountClicked: () -> Unit,
    pendingInvitations: List<AccountSelectorViewModel.Invitation>,
    onAcceptInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
    onRejectInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
) {
    if (pendingInvitations.isNotEmpty()) {
        Text(
            text = "Pending invitations",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        for(invitation in pendingInvitations) {
            InvitationView(
                invitation = invitation,
                onRejectInvitationConfirmed = onRejectInvitationConfirmed,
                onAcceptInvitationConfirmed = onAcceptInvitationConfirmed,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Text(
        text = "Your accounts",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(modifier = Modifier.height(16.dp))

    for(account in ownAccounts) {
        AccountButton(
            title = account.name,
            subtitle = null,
            enabled = true,
            selected = account.selected,
            onClick = { onAccountSelected(account) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (ownAccounts.isEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (showCreateOnlineAccountButton) {
        SmallFloatingActionButton(
            onClick = onCreateAccountClicked,
            modifier = Modifier.align(Alignment.End),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Text("+")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (invitedAccounts.isNotEmpty()) {
        Text(
            text = "Other accounts",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        for(account in invitedAccounts) {
            AccountButton(
                title = account.name,
                subtitle = account.ownerEmail,
                enabled = true,
                selected = account.selected,
                onClick = { onAccountSelected(account) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
private fun ColumnScope.ErrorView(
    error: Throwable,
    onRetryButtonClicked: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "Unable to fetch online accounts",
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "An error occurred while fetching your online accounts. Please check your network and try again.\n(${error.localizedMessage})",
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
private fun AccountButton(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable (
                enabled = enabled,
                onClick = onClick,
            ),
        tonalElevation = if (enabled) { 3.dp } else { 2.dp },
        shadowElevation = if (enabled) { 4.dp } else { 0.5.dp },
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = colorResource(R.color.secondary_text),
                    )
                }
            }

        }

    }
}

@Composable
private fun InvitationView(
    invitation: AccountSelectorViewModel.Invitation,
    onRejectInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
    onAcceptInvitationConfirmed: (AccountSelectorViewModel.Invitation) -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp),
        ) {
            Text(
                text = invitation.account.name,
                fontSize = 16.sp,
            )

            Text(
                text = invitation.account.ownerEmail,
                fontSize = 15.sp,
                color = colorResource(R.color.secondary_text),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (invitation.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            MaterialAlertDialogBuilder(context)
                                .setTitle("Reject invitation")
                                .setMessage("Are you sure you want to reject this account invitation? You'll need to be invited again to be able to access it.")
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Reject") { dialog, _ ->
                                    onRejectInvitationConfirmed(invitation)
                                    dialog.dismiss()
                                }
                                .show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(
                            text = "Reject",
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            onAcceptInvitationConfirmed(invitation)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
@Preview(name = "Loading preview")
fun AccountsLoadingViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.Loading,
            onIabErrorRetryButtonClicked = {},
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
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
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
        )
    }
}

@Composable
@Preview(name = "Not pro preview")
fun AccountsNotProViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.NotPro(
                isOfflineBackupEnabled = false,
            ),
            onIabErrorRetryButtonClicked = {},
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
        )
    }
}

@Composable
@Preview(name = "Not authenticated")
fun AccountsNotAuthenticatedViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.NotAuthenticated(
                isOfflineBackupEnabled = false,
            ),
            onIabErrorRetryButtonClicked = {},
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
        )
    }
}

@Composable
@Preview(name = "Accounts available preview")
fun AccountsAvailableViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.AccountsAvailable(
                userEmail = "test@email.com",
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
                showCreateOnlineAccountButton = true,
                invitedAccounts = listOf(
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = false,
                        name = "Other person account",
                        ownerEmail = "other.person@gmail.com",
                    ),
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = false,
                        name = "Other account 2 with a super long name to test how it looks and see how the cell behaves",
                        ownerEmail = "other.person.withasuperlongemailoiqoisqdohqsolihqsdoiqshdoqisdhqsdoihsdqoihqsdiouhhqohidqsh@gmail.com",
                    )
                ),
                pendingInvitations = listOf(),
                isOfflineBackupEnabled = true,
            ),
            onIabErrorRetryButtonClicked = {},
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
        )
    }
}

@Composable
@Preview(name = "Accounts available full preview")
fun AccountsAvailableFullViewPreview() {
    AppTheme {
        AccountsView(
            state = AccountSelectorViewModel.State.AccountsAvailable(
                userEmail = "test@email.com",
                isOfflineSelected = false,
                ownAccounts = listOf(
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = true,
                        name = "Own account 1",
                        ownerEmail = "",
                    ),
                ),
                showCreateOnlineAccountButton = false,
                invitedAccounts = listOf(
                    AccountSelectorViewModel.Account(
                        id = "",
                        secret = "",
                        selected = false,
                        name = "Other person account",
                        ownerEmail = "other.person@gmail.com",
                    ),
                ),
                pendingInvitations = listOf(
                    AccountSelectorViewModel.Invitation(
                        account = AccountSelectorViewModel.Account(
                            id = "",
                            secret = "",
                            selected = false,
                            name = "Other person account",
                            ownerEmail = "other.person@gmail.com",
                        ),
                        isLoading = true,
                        user = CurrentUser("", "", ""),
                    ),
                    AccountSelectorViewModel.Invitation(
                        account = AccountSelectorViewModel.Account(
                            id = "",
                            secret = "",
                            selected = false,
                            name = "Other person account 2",
                            ownerEmail = "other.person@gmail.com",
                        ),
                        isLoading = false,
                        user = CurrentUser("", "", ""),
                    ),
                    AccountSelectorViewModel.Invitation(
                        account = AccountSelectorViewModel.Account(
                            id = "",
                            secret = "",
                            selected = false,
                            name = "Other person account with a super long name to test how it looks on multiple lines to make sure it's ok",
                            ownerEmail = "other.person.with.a.super.long.email.that.nobody.can.type@gmail.com",
                        ),
                        isLoading = false,
                        user = CurrentUser("", "", ""),
                    )
                ),
                isOfflineBackupEnabled = false,
            ),
            onIabErrorRetryButtonClicked = {},
            onErrorRetryButtonClicked = {},
            onAccountSelected = {},
            onBecomeProButtonClicked = {},
            onLoginButtonPressed = {},
            onCreateAccountClicked = {},
            onRejectInvitationConfirmed = {},
            onAcceptInvitationConfirmed = {},
        )
    }
}