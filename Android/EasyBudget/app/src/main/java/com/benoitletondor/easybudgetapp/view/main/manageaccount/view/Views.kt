package com.benoitletondor.easybudgetapp.view.main.manageaccount.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.accounts.model.InvitationStatus
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.manageaccount.LoadingKind
import com.benoitletondor.easybudgetapp.view.main.manageaccount.ManageAccountViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Composable
fun ContentView(
    state: ManageAccountViewModel.State,
    onUpdateAccountNameClicked: (String) -> Unit,
    onInvitationDeleteConfirmed: (Invitation) -> Unit,
    onRetryButtonClicked: () -> Unit,
    onLeaveAccountConfirmed: () -> Unit,
) {
    when(state) {
        ManageAccountViewModel.State.DeletingInvitation -> LoadingView(kind = LoadingKind.DELETING_INVITATION)
        is ManageAccountViewModel.State.Error -> ErrorView(
            error = state.error,
            onRetryButtonClicked = onRetryButtonClicked,
        )
        ManageAccountViewModel.State.Loading -> LoadingView(kind = LoadingKind.LOADING_DATA)
        is ManageAccountViewModel.State.Ready.Invited -> ManageAccountAsInvitedView(
            accountName = state.accountName,
            onLeaveAccountConfirmed = onLeaveAccountConfirmed,
        )
        is ManageAccountViewModel.State.Ready.Owner -> ManageAccountAsOwnerView(
            initialNameValue = state.accountName,
            invitationsSent = state.invitationsSent,
            invitationsAccepted = state.invitationsAccepted,
            onUpdateAccountNameClicked = onUpdateAccountNameClicked,
            onInvitationDeleteConfirmed = onInvitationDeleteConfirmed,
        )
        ManageAccountViewModel.State.SendingInvitation -> LoadingView(kind = LoadingKind.SENDING_INVITATION)
        ManageAccountViewModel.State.Updating -> LoadingView(kind = LoadingKind.UPDATING_NAME)
        ManageAccountViewModel.State.DeletingAccount -> TODO()
        ManageAccountViewModel.State.LeavingAccount -> TODO()
    }
}

@Composable
private fun ManageAccountAsInvitedView(
    accountName: String,
    onLeaveAccountConfirmed: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Account name",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = accountName,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Leave account")
                    .setMessage("Are you sure you want to leave the account? You won't be able to access it anymore until you're invited again by the owner.")
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Leave") { dialog, _ ->
                        onLeaveAccountConfirmed()
                        dialog.dismiss()
                    }
                    .show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.budget_red),
            )
        ) {
            Text("Leave account")
        }
    }
}

@Composable
private fun ManageAccountAsOwnerView(
    initialNameValue: String,
    invitationsSent: List<Invitation>,
    invitationsAccepted: List<Invitation>,
    onUpdateAccountNameClicked: (String) -> Unit,
    onInvitationDeleteConfirmed: (Invitation) -> Unit,
) {
    var accountName by remember { mutableStateOf(initialNameValue) }
    val isAccountNameValid = accountName.length in 1..49
    val shouldDisplayAccountNameError = accountName.length >= 50

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Account name:",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = accountName,
            onValueChange = { newValue ->
                accountName = newValue
            },
            placeholder = {
                Text("Savings account, joint account, ...")
            },
            isError = shouldDisplayAccountNameError,
            supportingText = {
                if (shouldDisplayAccountNameError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Name should be less than 50 chars",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                if (isAccountNameValid) {
                    onUpdateAccountNameClicked(accountName)
                }
            },
            enabled = isAccountNameValid,
        ) {
            Text("Update")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Invitations",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        for(invitation in invitationsSent) {
            InvitationRow(
                invitation = invitation,
                onDeleteConfirmed = onInvitationDeleteConfirmed,
            )
        }

        for(invitation in invitationsAccepted) {
            InvitationRow(
                invitation = invitation,
                onDeleteConfirmed = onInvitationDeleteConfirmed,
            )
        }
    }
}

@Composable
private fun InvitationRow(
    invitation: Invitation,
    onDeleteConfirmed: (Invitation) -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = invitation.receiverEmail,
                    fontSize = 16.sp,
                )

                Text(
                    text = when(invitation.status) {
                        InvitationStatus.SENT -> "Sent"
                        InvitationStatus.ACCEPTED -> "Accepted"
                        InvitationStatus.REJECTED -> "Rejected"
                    },
                    fontSize = 15.sp,
                    color = colorResource(R.color.secondary_text),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Image(
                painter = painterResource(R.drawable.ic_baseline_delete_24),
                contentDescription = "Delete",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Delete invitation")
                            .setMessage("Are you sure you want to delete this invitation? ${invitation.receiverEmail} won't be able to access the account anymore.")
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton("Delete") { dialog, _ ->
                                onDeleteConfirmed(invitation)
                                dialog.dismiss()
                            }
                            .show()
                    }
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: Throwable,
    onRetryButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Unable to account details",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "An error occurred while fetching account's details. Please check your network and try again.\n(${error.localizedMessage})",
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

}

@Composable
private fun LoadingView(
    kind: LoadingKind,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = when(kind) {
                LoadingKind.LOADING_DATA -> "Loading..."
                LoadingKind.DELETING_INVITATION -> "Deleting invitation..."
                LoadingKind.SENDING_INVITATION -> "Sending invitation..."
                LoadingKind.UPDATING_NAME -> "Updating..."
            },
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun LoadingStatePreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.Loading,
            onUpdateAccountNameClicked = {},
            onInvitationDeleteConfirmed = {},
            onRetryButtonClicked = {},
            onLeaveAccountConfirmed = {},
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun UpdatingStatePreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.Updating,
            onUpdateAccountNameClicked = {},
            onInvitationDeleteConfirmed = {},
            onRetryButtonClicked = {},
            onLeaveAccountConfirmed = {},
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun DeletingInvitationStatePreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.DeletingInvitation,
            onUpdateAccountNameClicked = {},
            onInvitationDeleteConfirmed = {},
            onRetryButtonClicked = {},
            onLeaveAccountConfirmed = {},
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun SendingInvitationStatePreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.SendingInvitation,
            onUpdateAccountNameClicked = {},
            onInvitationDeleteConfirmed = {},
            onRetryButtonClicked = {},
            onLeaveAccountConfirmed = {},
        )
    }
}