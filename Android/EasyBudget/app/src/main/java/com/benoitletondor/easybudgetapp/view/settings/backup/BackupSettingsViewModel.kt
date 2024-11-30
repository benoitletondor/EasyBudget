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

package com.benoitletondor.easybudgetapp.view.settings.backup

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.parameters.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import java.lang.RuntimeException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val auth: Auth,
    private val parameters: Parameters,
    private val cloudStorage: CloudStorage,
    private val iab: Iab,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private val backupInProgressFlow = MutableStateFlow(false)
    private val restorationInProgressFlow = MutableStateFlow(false)
    private val deletionInProgressFlow = MutableStateFlow(false)

    val stateFlow: StateFlow<State> = combine(
        auth.state
            .onEach { authState ->
                if (authState is AuthState.Authenticated) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            if (parameters.getLastBackupDate() == null) {
                                getBackupDBMetaData(cloudStorage, auth)?.let {
                                    parameters.saveLastBackupDate(it.lastUpdateDate)
                                }
                            }
                        } catch (e: Throwable) {
                            if (e is CancellationException) throw e

                            Logger.error("Error getting last backup date", e)
                        }
                    }
                }
            },
        parameters.watchIsBackupEnabled(),
        parameters.watchLastBackupDate(),
        getBackupJobInfosFlow(appContext)
            .onStart {
                emit(emptyList())
            },
        backupInProgressFlow,
        restorationInProgressFlow,
        deletionInProgressFlow,
    ) { authState, backupEnabled, lastBackupDate, _, backupInProgress, restorationInProgress, deletionInProgress ->
        return@combine when (authState) {
            AuthState.NotAuthenticated -> State.NotAuthenticated
            AuthState.Authenticating -> State.Authenticating
            is AuthState.Authenticated -> {
                if (backupInProgress) {
                    State.BackupInProgress(authState.currentUser)
                } else if (restorationInProgress) {
                    State.RestorationInProgress(authState.currentUser)
                } else if (deletionInProgress) {
                    State.DeletionInProgress(authState.currentUser)
                } else {
                    if (backupEnabled) {
                        val backupNowAvailable =
                            lastBackupDate == null || lastBackupDate.isOlderThanADay()
                        val restoreAvailable = lastBackupDate != null

                        State.Activated(
                            authState.currentUser,
                            lastBackupDate,
                            backupNowAvailable,
                            restoreAvailable
                        )
                    } else {
                        State.NotActivated(authState.currentUser)
                    }
                }

            }
        }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.NotAuthenticated)

    fun onAuthenticateButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowAuthenticationConfirmation)
        }
    }

    fun onAuthenticationConfirmationConfirmed(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        auth.startAuthentication(launcher)
    }

    fun onAuthenticationConfirmationCancelled() {
        // No-op
    }

    fun handleAuthActivityResult(activityResult: ActivityResult) {
        auth.handleActivityResult(activityResult.resultCode, activityResult.data)
    }

    fun onLogoutButtonPressed() {
        parameters.saveLastBackupDate(null)
        parameters.setBackupEnabled(false)

        viewModelScope.launch {
            try {
                unscheduleBackup(appContext)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while unscheduling backup", e)
            }
        }

        auth.logout()
    }

    fun onBackupActivated() {
        if (!parameters.isBackupEnabled()) {
            parameters.setBackupEnabled(true)
            Logger.debug("Backup activated")

            viewModelScope.launch {
                val maybeBackupActivatedState = withTimeoutOrNull(5.seconds) {
                    stateFlow.filterIsInstance<State.Activated>().first()
                }

                val lastBackupDate = maybeBackupActivatedState?.lastBackupDate
                if (lastBackupDate != null) {
                    viewModelScope.launch {
                        eventMutableFlow.emit(Event.PromptUserToRestorePreviousBackup(lastBackupDate))
                    }
                }

                try {
                    scheduleBackup(appContext)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Logger.error("Error while scheduling backup", e)
                }
            }
        }
    }

    fun onBackupDeactivated() {
        if (parameters.isBackupEnabled()) {
            parameters.setBackupEnabled(false)

            viewModelScope.launch {
                try {
                    unscheduleBackup(appContext)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Logger.error("Error while unscheduling backup", e)
                }
            }

            Logger.debug("Backup deactivated")
        }
    }

    fun onBackupNowButtonPressed() {
        viewModelScope.launch {
            backupInProgressFlow.value = true

            try {
                withContext(Dispatchers.IO) {
                    val result = backupDB(
                        appContext,
                        cloudStorage,
                        auth,
                        parameters,
                        iab,
                    )

                    if (result !is ListenableWorker.Result.Success) {
                        throw RuntimeException(result.toString())
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error

                Logger.error("Error while backup now", error)
                eventMutableFlow.emit(Event.ShowBackupNowError(error))
            } finally {
                backupInProgressFlow.value = false
            }
        }
    }

    fun onRestorePreviousBackupButtonPressed() {
        startRestoreFlow()
    }

    fun onIgnorePreviousBackupButtonPressed() {
        // No-op
    }

    fun onRestoreButtonPressed() {
        startRestoreFlow()
    }

    fun onRestoreBackupConfirmationConfirmed() {
        restoreData()
    }

    fun onRestoreBackupConfirmationCancelled() {
        // No-op
    }

    fun onDeleteBackupButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowDeleteConfirmation)
        }
    }

    fun onDeleteBackupConfirmationConfirmed() {
        viewModelScope.launch {
            deletionInProgressFlow.value = true

            try {
                withContext(Dispatchers.IO) {
                    deleteBackup(auth, cloudStorage, iab)
                    parameters.saveLastBackupDate(null)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error

                Logger.error("Error while deleting backup", error)
                eventMutableFlow.emit(Event.ShowBackupDeletionError(error))
            } finally {
                deletionInProgressFlow.value = false
            }
        }
    }

    fun onDeleteBackupConfirmationCancelled() {
        // No-op
    }

    private fun startRestoreFlow() {
        val lastBackupDate = (stateFlow.value as? State.Activated)?.lastBackupDate
        if (lastBackupDate == null) {
            Logger.error(
                "Starting restore with no last backup date",
                NullPointerException("No last backup date")
            )
            return
        }

        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowRestoreConfirmation(lastBackupDate))
        }
    }

    private fun restoreData() {
        viewModelScope.launch {
            restorationInProgressFlow.value = true

            try {
                withContext(Dispatchers.IO) {
                    restoreLatestDBBackup(appContext, auth, cloudStorage, iab, parameters)
                }

                eventMutableFlow.emit(Event.RestartApp)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error

                Logger.error("Error while restoring", error)
                eventMutableFlow.emit(Event.ShowRestoreError(error))
            } finally {
                restorationInProgressFlow.value = false
            }
        }
    }

    private fun Date.isOlderThanADay(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        return calendar.time.after(this)
    }

    sealed class Event {
        data object ShowAuthenticationConfirmation : Event()
        data class ShowBackupNowError(val error: Throwable) : Event()
        data class ShowRestoreError(val error: Throwable) : Event()
        data object RestartApp : Event()
        data class ShowRestoreConfirmation(val lastBackupDate: Date) : Event()
        data object ShowDeleteConfirmation : Event()
        data class ShowBackupDeletionError(val error: Throwable) : Event()
        data class PromptUserToRestorePreviousBackup(val lastBackupDate: Date) : Event()
    }

    sealed class State {
        data object NotAuthenticated : State()
        data object Authenticating : State()
        sealed interface Authenticated {
            val currentUser: CurrentUser
        }

        data class NotActivated(override val currentUser: CurrentUser) : State(), Authenticated
        data class Activated(
            override val currentUser: CurrentUser,
            val lastBackupDate: Date?,
            val backupNowAvailable: Boolean,
            val restoreAvailable: Boolean
        ) : State(), Authenticated

        data class BackupInProgress(override val currentUser: CurrentUser) : State(), Authenticated
        data class RestorationInProgress(override val currentUser: CurrentUser) : State(), Authenticated
        data class DeletionInProgress(override val currentUser: CurrentUser) : State(), Authenticated
    }

}