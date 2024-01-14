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

package com.benoitletondor.easybudgetapp.view.main.manageaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityCreateAccountBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.manageaccount.view.ContentView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageAccountActivity : BaseActivity<ActivityCreateAccountBinding>() {
    private val viewModel: ManageAccountViewModel by viewModels()

    override fun createBinding(): ActivityCreateAccountBinding = ActivityCreateAccountBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.createAccountComposeView.setContent {
            AppTheme {
                val state by viewModel.stateFlow.collectAsState()

                ContentView(
                    state = state,
                    onUpdateAccountNameClicked = viewModel::onUpdateAccountNameClicked,
                    onInvitationDeleteConfirmed = viewModel::onInvitationDeleteConfirmed,
                    onRetryButtonClicked = viewModel::onRetryButtonClicked,
                    onLeaveAccountConfirmed = viewModel::onLeaveAccountConfirmed,
                    onInviteEmailToAccount = viewModel::onInviteEmailToAccount,
                    onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
                )
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                ManageAccountViewModel.Event.AccountLeft -> Toast.makeText(this, R.string.account_management_account_left_confirmation, Toast.LENGTH_LONG).show()
                ManageAccountViewModel.Event.AccountNameUpdated -> Toast.makeText(this, R.string.account_management_account_name_updated_confirmation, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.ErrorDeletingInvitation -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(getString(R.string.account_management_error_deleting_invitation, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorUpdatingAccountName -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(getString(R.string.account_management_error_updating_name, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorWhileInviting -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(getString(R.string.account_management_error_sending_invitation, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                is ManageAccountViewModel.Event.ErrorWhileLeavingAccount -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(getString(R.string.account_management_error_leaving_account, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                ManageAccountViewModel.Event.Finish -> finish()
                is ManageAccountViewModel.Event.InvitationDeleted -> Toast.makeText(this, R.string.account_management_invitation_revoked, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.InvitationSent -> Toast.makeText(this, R.string.account_management_invitation_sent, Toast.LENGTH_LONG).show()
                ManageAccountViewModel.Event.AccountDeleted -> Toast.makeText(this, R.string.account_management_account_deleted, Toast.LENGTH_LONG).show()
                is ManageAccountViewModel.Event.ErrorWhileDeletingAccount -> MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.account_management_error_title)
                    .setMessage(getString(R.string.account_management_error_deleting_account, event.error.localizedMessage))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val SELECTED_ACCOUNT_EXTRA = "selectedAccount"
        
        fun newIntent(
            context: Context,
            selectedAccount: MainViewModel.SelectedAccount.Selected.Online,
        ): Intent {
            return Intent(context, ManageAccountActivity::class.java).apply {
                putExtra(SELECTED_ACCOUNT_EXTRA, selectedAccount)
            }
        }
    }
}

enum class LoadingKind {
    LOADING_DATA,
    DELETING_INVITATION,
    SENDING_INVITATION,
    UPDATING_NAME,
    DELETING_ACCOUNT,
    LEAVING_ACCOUNT,
}

