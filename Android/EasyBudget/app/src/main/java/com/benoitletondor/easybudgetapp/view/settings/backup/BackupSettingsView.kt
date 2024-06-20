package com.benoitletondor.easybudgetapp.view.settings.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.premium.view.LoadingView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

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
    )
}

@Composable
private fun BackupSettingsView(
    stateFlow: StateFlow<BackupSettingsViewModel.State>,
    eventFlow: Flow<BackupSettingsViewModel.Event>,
    navigateUp: () -> Unit,
) {
    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                is BackupSettingsViewModel.Event.PromptUserToRestoreBackup -> TODO()
                BackupSettingsViewModel.Event.RestartApp -> TODO()
                BackupSettingsViewModel.Event.ShowAuthenticationConfirmation -> TODO()
                is BackupSettingsViewModel.Event.ShowBackupDeletionError -> TODO()
                is BackupSettingsViewModel.Event.ShowBackupNowError -> TODO()
                BackupSettingsViewModel.Event.ShowDeleteConfirmation -> TODO()
                is BackupSettingsViewModel.Event.ShowRestoreConfirmation -> TODO()
                is BackupSettingsViewModel.Event.ShowRestoreError -> TODO()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.backup_settings_activity_title),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding),
            ) {
                val state by stateFlow.collectAsState()

                when(val currentState = state) {
                    BackupSettingsViewModel.State.NotAuthenticated -> TODO()
                    BackupSettingsViewModel.State.Authenticating -> LoadingView()
                    is BackupSettingsViewModel.State.Activated -> TODO()
                    is BackupSettingsViewModel.State.NotActivated -> TODO()
                    is BackupSettingsViewModel.State.RestorationInProgress -> TODO()
                    is BackupSettingsViewModel.State.BackupInProgress -> TODO()
                    is BackupSettingsViewModel.State.DeletionInProgress -> TODO()
                }
            }
        },
    )
}