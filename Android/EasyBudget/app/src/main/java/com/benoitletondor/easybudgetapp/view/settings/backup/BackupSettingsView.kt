/*
 *   Copyright 2025 Benoit Letondor
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
package com.benoitletondor.easybudgetapp.view.settings.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.settings.backup.subviews.AuthenticatedView
import com.benoitletondor.easybudgetapp.view.settings.backup.subviews.NotAuthenticatedView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.util.Date
import kotlin.system.exitProcess

@Serializable
object BackupSettingsDestination

@Composable
fun BackupSettingsView(
    viewModel: BackupSettingsViewModel = hiltViewModel(),
    navigateUp: () -> Unit,
) {
    BackupSettingsView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        onLoginButtonClicked = viewModel::onAuthenticateButtonPressed,
        onAuthenticationConfirmationConfirmed = viewModel::onAuthenticationConfirmationConfirmed,
        onAuthenticationConfirmationCancelled = viewModel::onAuthenticationConfirmationCancelled,
        onRestoreBackupConfirmationConfirmed = viewModel::onRestoreBackupConfirmationConfirmed,
        onRestoreBackupConfirmationCancelled = viewModel::onRestoreBackupConfirmationCancelled,
        onDeleteBackupConfirmationConfirmed = viewModel::onDeleteBackupConfirmationConfirmed,
        onDeleteBackupConfirmationCancelled = viewModel::onDeleteBackupConfirmationCancelled,
        onAuthActivityResult = viewModel::handleAuthActivityResult,
        onLogoutButtonClicked = viewModel::onLogoutButtonPressed,
        onBackupActivationChange = { backupActivated ->
            if (backupActivated) {
                viewModel.onBackupActivated()
            } else {
                viewModel.onBackupDeactivated()
            }
        },
        onBackupNowClicked = viewModel::onBackupNowButtonPressed,
        onRestoreNowClicked = viewModel::onRestoreButtonPressed,
        onDeleteBackupClicked = viewModel::onDeleteBackupButtonPressed,
        onRestorePreviousBackupButtonPressed = viewModel::onRestorePreviousBackupButtonPressed,
        onIgnorePreviousBackupButtonPressed = viewModel::onIgnorePreviousBackupButtonPressed,
    )
}

@Composable
private fun BackupSettingsView(
    stateFlow: StateFlow<BackupSettingsViewModel.State>,
    eventFlow: Flow<BackupSettingsViewModel.Event>,
    navigateUp: () -> Unit,
    onLoginButtonClicked: () -> Unit,
    onAuthenticationConfirmationConfirmed: (ManagedActivityResultLauncher<Intent, ActivityResult>) -> Unit,
    onAuthenticationConfirmationCancelled: () -> Unit,
    onRestoreBackupConfirmationConfirmed: () -> Unit,
    onRestoreBackupConfirmationCancelled: () -> Unit,
    onDeleteBackupConfirmationConfirmed: () -> Unit,
    onDeleteBackupConfirmationCancelled: () -> Unit,
    onAuthActivityResult: (ActivityResult) -> Unit,
    onLogoutButtonClicked: () -> Unit,
    onBackupActivationChange: (Boolean) -> Unit,
    onBackupNowClicked: () -> Unit,
    onRestoreNowClicked: () -> Unit,
    onDeleteBackupClicked: () -> Unit,
    onRestorePreviousBackupButtonPressed: () -> Unit,
    onIgnorePreviousBackupButtonPressed: () -> Unit,
) {
    val context = LocalContext.current

    val authActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        onAuthActivityResult(result)
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                is BackupSettingsViewModel.Event.PromptUserToRestorePreviousBackup -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_already_exist_title)
                        .setMessage(
                            context.getString(
                                R.string.backup_already_exist_message,
                                event.lastBackupDate.formatLastBackupDate(context),
                            )
                        )
                        .setPositiveButton(R.string.backup_restore_confirmation_positive_cta) { _, _ ->
                            onRestorePreviousBackupButtonPressed()
                        }
                        .setNegativeButton(R.string.backup_restore_confirmation_negative_cta) { _, _ ->
                            onIgnorePreviousBackupButtonPressed()
                        }
                        .show()
                }
                BackupSettingsViewModel.Event.RestartApp -> {
                    val activity = context as Activity
                    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                    activity.finishAffinity()
                    activity.startActivity(intent)
                    exitProcess(0)
                }
                BackupSettingsViewModel.Event.ShowAuthenticationConfirmation -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_settings_not_authenticated_privacy_title)
                        .setMessage(R.string.backup_settings_not_authenticated_privacy_message)
                        .setPositiveButton(R.string.backup_settings_not_authenticated_privacy_positive_cta) { _, _ ->
                            onAuthenticationConfirmationConfirmed(authActivityLauncher)
                        }
                        .setNegativeButton(R.string.backup_settings_not_authenticated_privacy_negative_cta) { _, _ ->
                            onAuthenticationConfirmationCancelled()
                        }
                        .show()
                }
                is BackupSettingsViewModel.Event.ShowBackupDeletionError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_wipe_data_error_title)
                        .setMessage(R.string.backup_wipe_data_error_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                is BackupSettingsViewModel.Event.ShowBackupNowError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_now_error_title)
                        .setMessage(R.string.backup_now_error_message)
                        .setPositiveButton(android.R.string.ok, null)
                }
                BackupSettingsViewModel.Event.ShowDeleteConfirmation -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_wipe_data_confirmation_title)
                        .setMessage(R.string.backup_wipe_data_confirmation_message)
                        .setPositiveButton(R.string.backup_wipe_data_confirmation_positive_cta) { _, _ ->
                            onDeleteBackupConfirmationConfirmed()
                        }
                        .setNegativeButton(R.string.backup_wipe_data_confirmation_negative_cta) { _, _ ->
                            onDeleteBackupConfirmationCancelled()
                        }
                        .show()
                }
                is BackupSettingsViewModel.Event.ShowRestoreConfirmation -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_restore_confirmation_title)
                        .setMessage(
                            context.getString(
                                R.string.backup_restore_confirmation_message,
                                event.lastBackupDate.formatLastBackupDate(context),
                            )
                        )
                        .setPositiveButton(R.string.backup_restore_confirmation_positive_cta) { _, _ ->
                            onRestoreBackupConfirmationConfirmed()
                        }
                        .setNegativeButton(R.string.backup_restore_confirmation_negative_cta) { _, _ ->
                            onRestoreBackupConfirmationCancelled()
                        }
                        .show()
                }
                is BackupSettingsViewModel.Event.ShowRestoreError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.backup_restore_error_title)
                        .setMessage(R.string.backup_restore_error_message)
                        .setPositiveButton(android.R.string.ok, null)
                }
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.backup_settings_activity_title),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.backup_settings_cloud_backup),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                val state by stateFlow.collectAsState()
                when(val currentState = state) {
                    BackupSettingsViewModel.State.NotAuthenticated -> NotAuthenticatedView(
                        onLoginButtonClicked = onLoginButtonClicked,
                    )
                    BackupSettingsViewModel.State.Authenticating -> LoadingView()
                    is BackupSettingsViewModel.State.Authenticated -> {
                        AuthenticatedView(
                            state = currentState,
                            onLogoutButtonClicked = onLogoutButtonClicked,
                            onBackupActivationChange = onBackupActivationChange,
                            onBackupNowClicked = onBackupNowClicked,
                            onRestoreNowClicked = onRestoreNowClicked,
                            onDeleteBackupClicked = onDeleteBackupClicked,
                        )
                    }
                }
            }
        },
    )
}

private fun Date.formatLastBackupDate(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        this.time,
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
    )
}