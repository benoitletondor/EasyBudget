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

package com.benoitletondor.easybudgetapp.view.main.manageaccount.view

import android.util.Patterns
import android.view.WindowManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.benoitletondor.easybudgetapp.auth.CurrentUser
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
            text = stringResource(R.string.manage_account_invited_account_name_title),
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
                    .setTitle(R.string.manage_account_invited_leave_account_confirm_title)
                    .setMessage(R.string.manage_account_invited_leave_account_confirm_desc)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.manage_account_invited_leave_account_confirm_cta) { dialog, _ ->
                        onLeaveAccountConfirmed()
                        dialog.dismiss()
                    }
                    .show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.budget_red),
            )
        ) {
            Text(stringResource(R.string.manage_account_invited_leave_account_cta))
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
    val activity = context as? AppCompatActivity

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.manage_account_owner_account_name_title),
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
                Text(stringResource(R.string.create_account_account_name_placeholder))
            },
            isError = shouldDisplayAccountNameError,
            supportingText = {
                if (shouldDisplayAccountNameError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.create_account_account_name_too_large_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {
                if (isAccountNameValid) {
                    onUpdateAccountNameClicked(accountName)
                }
            },
            enabled = isAccountNameValid,
        ) {
            Text(stringResource(R.string.manage_account_owner_account_name_cta))
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.manage_account_owner_invitations_title),
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

                Spacer(modifier = Modifier.height(5.dp))
            }

            for(invitation in invitationsAccepted) {
                InvitationRow(
                    invitation = invitation,
                    onDeleteConfirmed = onInvitationDeleteConfirmed,
                )

                Spacer(modifier = Modifier.height(5.dp))
            }
        } else {
            Text(
                text = stringResource(R.string.manage_account_owner_invitation_empty_state),
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
                            val shouldDisplayEmailError = emailState.isNotEmpty() && !isEmailValid

                            AppTheme {
                                val focusRequester = remember { FocusRequester() }

                                TextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
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
                                        Text(stringResource(R.string.manage_account_owner_invitation_send_email))
                                    },
                                    isError = shouldDisplayEmailError,
                                    supportingText = {
                                        if (shouldDisplayEmailError) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = stringResource(R.string.manage_account_owner_invitation_send_error_bad_email),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                )

                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }

                    val dialog = MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.manage_account_owner_invitation_send_title)
                        .setMessage(R.string.manage_account_owner_invitation_send_desc)
                        .setView(composeView)
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setPositiveButton(R.string.manage_account_owner_invitation_send_cta) { dialog, _ ->
                            onInviteEmailToAccount(email)
                            dialog.dismiss()
                        }
                        .create()

                    dialog.window?.apply {
                        decorView.setViewTreeLifecycleOwner(activity)
                        decorView.setViewTreeSavedStateRegistryOwner(activity)
                    }

                    dialog.show()

                    // Allow text field to be focusable and keyboard to display
                    dialog.window?.apply {
                        clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                        clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    }
                },
                modifier = Modifier.align(Alignment.End),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("+")
            }
        } else {
            Text(
                text = stringResource(R.string.manage_account_owner_account_full_disclaimer),
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(id = R.color.secondary_text),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.manage_account_owner_danger_zone_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.manage_account_owner_delete_account_confirm_title)
                    .setMessage(R.string.manage_account_owner_delete_account_confirm_desc)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.manage_account_owner_delete_account_confirm_cta) { dialog, _ ->
                        onDeleteAccountConfirmed()
                        dialog.dismiss()
                    }
                    .show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.budget_red),
            )
        ) {
            Text(stringResource(R.string.manage_account_owner_delete_account_cta))
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
                        InvitationStatus.SENT -> stringResource(R.string.manage_account_owner_invitation_status_sent)
                        InvitationStatus.ACCEPTED -> stringResource(R.string.manage_account_owner_invitation_status_accepted)
                    },
                    fontSize = 15.sp,
                    color = colorResource(R.color.secondary_text),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Image(
                painter = painterResource(R.drawable.ic_baseline_delete_24),
                contentDescription = stringResource(R.string.manage_account_owner_invitation_delete_cta),
                colorFilter = ColorFilter.tint(colorResource(R.color.budget_red)),
                modifier = Modifier
                    .clickable {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.manage_account_owner_invitation_delete_confirm_title)
                            .setMessage(context.getString(R.string.manage_account_owner_invitation_delete_confirm_desc, invitation.receiverEmail))
                            .setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.manage_account_owner_invitation_delete_confirm_cta) { dialog, _ ->
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
            text = stringResource(R.string.manage_account_error_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.manage_account_error_desc, error.localizedMessage ?: error.toString()),
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
            text = stringResource(when(kind) {
                LoadingKind.LOADING_DATA -> R.string.manage_account_loading_generic
                LoadingKind.DELETING_INVITATION -> R.string.manage_account_loading_deleting_invitation
                LoadingKind.SENDING_INVITATION -> R.string.manage_account_loading_sending_invitation
                LoadingKind.UPDATING_NAME -> R.string.manage_account_loading_updating
                LoadingKind.DELETING_ACCOUNT -> R.string.manage_account_loading_deleting_account
                LoadingKind.LEAVING_ACCOUNT -> R.string.manage_account_loading_leaving_account
            }),
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
private fun OwnerStateEmptyInvitationsPreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.Ready.Owner(
                "accountName",
                CurrentUser("", "", ""),
                emptyList(),
                emptyList(),
            ),
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
private fun OwnerStateFullInvitationsPreview() {
    AppTheme {
        ContentView(
            state = ManageAccountViewModel.State.Ready.Owner(
                "accountName",
                CurrentUser("", "", ""),
                listOf(
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.SENT,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.SENT,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.SENT,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.SENT,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.SENT,
                        "fr_FR",
                    ),
                ),
                listOf(
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.ACCEPTED,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.ACCEPTED,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.ACCEPTED,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.ACCEPTED,
                        "fr_FR",
                    ),
                    Invitation(
                        "id",
                        "sender@email.com",
                        "senderId",
                        "receiver@email.com",
                        "accountId",
                        InvitationStatus.ACCEPTED,
                        "fr_FR",
                    ),
                ),
            ),
            onUpdateAccountNameClicked = {},
            onInvitationDeleteConfirmed = {},
            onRetryButtonClicked = {},
            onLeaveAccountConfirmed = {},
            onInviteEmailToAccount = {},
            onDeleteAccountConfirmed = {},
        )
    }
}
