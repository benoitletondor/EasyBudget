package com.benoitletondor.easybudgetapp.view.manageaccount

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.manageaccount.subviews.ContentView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class ManageAccountDestination(val selectedAccount: MainViewModel.SelectedAccount.Selected.Online)

@Composable
fun ManageAccountView(
    viewModel: ManageAccountViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    ManageAccountView(
        navigateUp = navigateUp,
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        onUpdateAccountNameClicked = viewModel::onUpdateAccountNameClicked,
        onInvitationDeleteConfirmed = viewModel::onInvitationDeleteConfirmed,
        onRetryButtonClicked = viewModel::onRetryButtonClicked,
        onLeaveAccountConfirmed = viewModel::onLeaveAccountConfirmed,
        onInviteEmailToAccount = viewModel::onInviteEmailToAccount,
        onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
        finish = finish,
    )
}

@Composable
private fun ManageAccountView(
    navigateUp: () -> Unit,
    stateFlow: StateFlow<ManageAccountViewModel.State>,
    eventFlow: Flow<ManageAccountViewModel.Event>,
    onUpdateAccountNameClicked: (String) -> Unit,
    onInvitationDeleteConfirmed: (Invitation) -> Unit,
    onRetryButtonClicked: () -> Unit,
    onLeaveAccountConfirmed: () -> Unit,
    onInviteEmailToAccount: (String) -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    finish: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                ManageAccountViewModel.Event.AccountLeft -> Toast.makeText(context, R.string.account_management_account_left_confirmation, Toast.LENGTH_LONG).show()
                ManageAccountViewModel.Event.AccountNameUpdated -> Toast.makeText(context, R.string.account_management_account_name_updated_confirmation, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.ErrorDeletingInvitation -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(context.getString(R.string.account_management_error_deleting_invitation, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorUpdatingAccountName -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(context.getString(R.string.account_management_error_updating_name, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorWhileInviting -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(context.getString(R.string.account_management_error_sending_invitation, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorWhileLeavingAccount -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(context.getString(R.string.account_management_error_leaving_account, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                ManageAccountViewModel.Event.Finish -> finish()
                is ManageAccountViewModel.Event.InvitationDeleted -> Toast.makeText(context, R.string.account_management_invitation_revoked, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.InvitationSent -> Toast.makeText(context, R.string.account_management_invitation_sent, Toast.LENGTH_LONG).show()
                ManageAccountViewModel.Event.AccountDeleted -> Toast.makeText(context, R.string.account_management_account_deleted, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.ErrorWhileDeletingAccount -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(context.getString(R.string.account_management_error_deleting_account, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_manage_account),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            val state by stateFlow.collectAsState()

            ContentView(
                modifier = Modifier.padding(contentPadding),
                state = state,
                onUpdateAccountNameClicked = onUpdateAccountNameClicked,
                onInvitationDeleteConfirmed = onInvitationDeleteConfirmed,
                onRetryButtonClicked = onRetryButtonClicked,
                onLeaveAccountConfirmed = onLeaveAccountConfirmed,
                onInviteEmailToAccount = onInviteEmailToAccount,
                onDeleteAccountConfirmed = onDeleteAccountConfirmed,
            )
        }
    )
}