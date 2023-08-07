package com.benoitletondor.easybudgetapp.view.main.manageaccount.view

import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
    onInviteEmailToAccount: (String) -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
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
            onInviteEmailToAccount = onInviteEmailToAccount,
            onDeleteAccountConfirmed = onDeleteAccountConfirmed,
        )
        ManageAccountViewModel.State.SendingInvitation -> LoadingView(kind = LoadingKind.SENDING_INVITATION)
        ManageAccountViewModel.State.Updating -> LoadingView(kind = LoadingKind.UPDATING_NAME)
        ManageAccountViewModel.State.DeletingAccount ->  LoadingView(kind = LoadingKind.DELETING_ACCOUNT)
        ManageAccountViewModel.State.LeavingAccount ->  LoadingView(kind = LoadingKind.LEAVING_ACCOUNT)
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
    onInviteEmailToAccount: (String) -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
) {
    var accountName by remember { mutableStateOf(initialNameValue) }
    val isAccountNameValid = accountName.length in 1..49
    val shouldDisplayAccountNameError = accountName.length >= 50

    val context = LocalContext.current
    val activity = context as AppCompatActivity

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

        if (invitationsSent.isNotEmpty() || invitationsAccepted.isNotEmpty()) {
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
        } else {
            Text(
                text = "No invitations sent yet",
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.secondary_text),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (invitationsSent.size + invitationsAccepted.size < 10) {
            SmallFloatingActionButton(
                onClick = {
                    var email = ""

                    val composeView = ComposeView(context).apply {
                        setContent {
                            var emailState by remember { mutableStateOf(email) }
                            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(emailState).matches()
                            val shouldDisplaEmailError = emailState.isNotEmpty() && !isEmailValid

                            AppTheme {
                                TextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                                    value = emailState,
                                    onValueChange = {
                                        emailState = it
                                        email = it
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        keyboardType = KeyboardType.Email,
                                        capitalization = KeyboardCapitalization.None,
                                    ),
                                    label = {
                                        Text("Email")
                                    },
                                    isError = shouldDisplaEmailError,
                                    supportingText = {
                                        if (shouldDisplaEmailError) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Please enter a valid email",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    val dialog = MaterialAlertDialogBuilder(context)
                        .setTitle("Invite to account")
                        .setMessage("Enter the email of the Google account of the EasyBudget Pro user you want to invite to this account.")
                        .setView(composeView)
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setPositiveButton("Invite") { dialog, _ ->
                            onInviteEmailToAccount(email)
                            dialog.dismiss()
                        }
                        .create()

                    dialog.window?.decorView?.setViewTreeLifecycleOwner(activity)
                    dialog.window?.decorView?.setViewTreeSavedStateRegistryOwner(activity)

                    dialog.show()
                },
                modifier = Modifier.align(Alignment.End),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("+")
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Danger zone",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Delete account")
                        .setMessage("Are you sure you want to delete the account? All the data will definitely be deleted, it's impossible to cancel.")
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setPositiveButton("Delete") { dialog, _ ->
                            onDeleteAccountConfirmed()
                            dialog.dismiss()
                        }
                        .show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.budget_red),
                )
            ) {
                Text("Delete account")
            }
        } else {
            Text(
                text = "This account is full, you can invite up to 5 person per online account.",
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(id = R.color.secondary_text),
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
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                    },
                    fontSize = 15.sp,
                    color = colorResource(R.color.secondary_text),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Image(
                painter = painterResource(R.drawable.ic_baseline_delete_24),
                contentDescription = "Delete",
                colorFilter = ColorFilter.tint(colorResource(R.color.budget_red)),
                modifier = Modifier
                    .clickable {
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Revoke invitation")
                            .setMessage("Are you sure you want to revoke this invitation? ${invitation.receiverEmail} won't be able to access the account anymore.")
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
                LoadingKind.DELETING_ACCOUNT -> "Deleting account..."
                LoadingKind.LEAVING_ACCOUNT -> "Leaving account..."
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
            onInviteEmailToAccount = {},
            onDeleteAccountConfirmed = {},
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
            onInviteEmailToAccount = {},
            onDeleteAccountConfirmed = {},
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
            onInviteEmailToAccount = {},
            onDeleteAccountConfirmed = {},
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
            onInviteEmailToAccount = {},
            onDeleteAccountConfirmed = {},
        )
    }
}