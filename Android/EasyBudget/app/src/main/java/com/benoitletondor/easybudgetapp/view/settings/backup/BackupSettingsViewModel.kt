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

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val auth: Auth,
    private val parameters: Parameters,
    private val cloudStorage: CloudStorage,
    private val iab: Iab,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val cloudBackupStateMutableFlow = MutableStateFlow<BackupCloudStorageState>(BackupCloudStorageState.NotAuthenticated)
    val cloudBackupStateFlow: Flow<BackupCloudStorageState> = cloudBackupStateMutableFlow

    private val authenticationConfirmationDisplayEventMutableFlow = MutableLiveFlow<Unit>()
    val authenticationConfirmationDisplayEventFlow : Flow<Unit> = authenticationConfirmationDisplayEventMutableFlow

    private val backupNowErrorEventMutableFlow = MutableLiveFlow<Throwable>()
    val backupNowErrorEventFlow: Flow<Throwable> = backupNowErrorEventMutableFlow

    private val restorationErrorEventMutableFlow = MutableLiveFlow<Throwable>()
    val restorationErrorEventFlow: Flow<Throwable> = restorationErrorEventMutableFlow

    private val previousBackupAvailableEventMutableFlow = MutableLiveFlow<Date>()
    val previousBackupAvailableEventFlow: Flow<Date> = previousBackupAvailableEventMutableFlow

    private val appRestartEventMutableFlow = MutableLiveFlow<Unit>()
    val appRestartEventFlow: Flow<Unit> = appRestartEventMutableFlow

    private val restoreConfirmationDisplayEventMutableFlow = MutableLiveFlow<Date>()
    val restoreConfirmationDisplayEventFlow: Flow<Date> = restoreConfirmationDisplayEventMutableFlow

    private val deleteConfirmationDisplayEventMutableFlow = MutableLiveFlow<Unit>()
    val deleteConfirmationDisplayEventFlow: Flow<Unit> = deleteConfirmationDisplayEventMutableFlow

    private val backupDeletionErrorEventMutableFlow = MutableLiveFlow<Throwable>()
    val backupDeletionErrorEventFlow: Flow<Throwable> = backupDeletionErrorEventMutableFlow

    private var backupInProgress = false
    private var restorationInProgress = false
    private var deletionInProgress = false

    init {
        viewModelScope.launchCollect(auth.state) { authState ->
            if( authState is AuthState.Authenticated ) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            if( parameters.getLastBackupDate() == null ) {
                                getBackupDBMetaData(cloudStorage, auth)?.let {
                                    parameters.saveLastBackupDate(it.lastUpdateDate)
                                }
                            }
                        } catch (e: Throwable) {
                            Logger.error(
                                "Error getting last backup date",
                                e
                            )
                        }

                        null // ?! not sure why it's needed
                    }

                    cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(authState)
                }
            } else {
                cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(authState)
            }
        }

        viewModelScope.launchCollect(getBackupJobInfosFlow(appContext)) {
            cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)
        }
    }

    fun onAuthenticateButtonPressed() {
        viewModelScope.launch {
            authenticationConfirmationDisplayEventMutableFlow.emit(Unit)
        }
    }

    fun onAuthenticationConfirmationConfirmed(activity: Activity) {
        auth.startAuthentication(activity)
    }

    fun onAuthenticationConfirmationCancelled() {
        // No-op
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        auth.handleActivityResult(requestCode, resultCode, data)
    }

    fun onLogoutButtonPressed() {
        parameters.saveLastBackupDate(null)
        parameters.setBackupEnabled(false)
        unscheduleBackup(appContext)

        auth.logout()
    }

    private fun computeBackupCloudStorageState(authState: AuthState?): BackupCloudStorageState {
        return when(authState) {
            AuthState.NotAuthenticated -> BackupCloudStorageState.NotAuthenticated
            AuthState.Authenticating -> BackupCloudStorageState.Authenticating
            is AuthState.Authenticated -> {
                if( backupInProgress ) {
                    BackupCloudStorageState.BackupInProgress(authState.currentUser)
                } else if ( restorationInProgress ) {
                    BackupCloudStorageState.RestorationInProgress(authState.currentUser)
                } else if ( deletionInProgress ) {
                    BackupCloudStorageState.DeletionInProgress(authState.currentUser)
                } else {
                    if( parameters.isBackupEnabled() ) {
                        val lastBackupDate = parameters.getLastBackupDate()
                        val backupNowAvailable = lastBackupDate == null || lastBackupDate.isOlderThanADay()
                        val restoreAvailable = lastBackupDate != null

                        BackupCloudStorageState.Activated(authState.currentUser, lastBackupDate, backupNowAvailable, restoreAvailable)
                    } else {
                        BackupCloudStorageState.NotActivated(authState.currentUser)
                    }
                }

            }
            null -> BackupCloudStorageState.NotAuthenticated
        }
    }

    fun onBackupActivated() {
        if( !parameters.isBackupEnabled() ) {
            parameters.setBackupEnabled(true)
            val newBackupState = computeBackupCloudStorageState(auth.state.value)
            cloudBackupStateMutableFlow.value = newBackupState

            if( newBackupState is BackupCloudStorageState.Activated ) {
                val lastBackupDate = newBackupState.lastBackupDate
                if( lastBackupDate != null ) {
                    viewModelScope.launch {
                        previousBackupAvailableEventMutableFlow.emit(lastBackupDate)
                    }
                }
            }

            scheduleBackup(appContext)
        }
    }

    fun onBackupDeactivated() {
        if( parameters.isBackupEnabled() ) {
            parameters.setBackupEnabled(false)
            cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)

            unscheduleBackup(appContext)
        }
    }

    fun onBackupNowButtonPressed() {
        viewModelScope.launch {
            backupInProgress = true
            cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    val result = backupDB(
                        appContext,
                        cloudStorage,
                        auth,
                        parameters,
                        iab,
                    )

                    if( result !is ListenableWorker.Result.Success ) {
                        throw RuntimeException(result.toString())
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error

                Logger.error("Error while backup now", error)
                backupNowErrorEventMutableFlow.emit(error)
            } finally {
                backupInProgress = false
                cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)
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
            deleteConfirmationDisplayEventMutableFlow.emit(Unit)
        }
    }

    fun onDeleteBackupConfirmationConfirmed() {
        viewModelScope.launch {
            deletionInProgress = true
            cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    deleteBackup(auth, cloudStorage, iab)
                    parameters.saveLastBackupDate(null)
                }
            } catch (error: Throwable) {
                Logger.error("Error while deleting backup", error)
                backupDeletionErrorEventMutableFlow.emit(error)
            } finally {
                deletionInProgress = false
                cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)
            }
        }
    }

    fun onDeleteBackupConfirmationCancelled() {
        // No-op
    }

    private fun startRestoreFlow() {
        val lastBackupDate = (cloudBackupStateMutableFlow.value as? BackupCloudStorageState.Activated)?.lastBackupDate
        if( lastBackupDate == null ) {
            Logger.error("Starting restore with no last backup date")
            return
        }

        viewModelScope.launch {
            restoreConfirmationDisplayEventMutableFlow.emit(lastBackupDate)
        }
    }

    private fun restoreData() {
        viewModelScope.launch {
            restorationInProgress = true
            cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    restoreLatestDBBackup(appContext, auth, cloudStorage, iab, parameters)
                }

                appRestartEventMutableFlow.emit(Unit)
            } catch (error: Throwable) {
                Logger.error("Error while restoring", error)
                restorationErrorEventMutableFlow.emit(error)
            } finally {
                restorationInProgress = false
                cloudBackupStateMutableFlow.value = computeBackupCloudStorageState(auth.state.value)
            }
        }
    }

    private fun Date.isOlderThanADay(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        return calendar.time.after(this)
    }
}

sealed class BackupCloudStorageState {
    data object NotAuthenticated : BackupCloudStorageState()
    data object Authenticating : BackupCloudStorageState()
    data class NotActivated(val currentUser: CurrentUser) : BackupCloudStorageState()
    data class Activated(val currentUser: CurrentUser,
                         val lastBackupDate: Date?,
                         val backupNowAvailable: Boolean,
                         val restoreAvailable: Boolean): BackupCloudStorageState()
    data class BackupInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
    data class RestorationInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
    data class DeletionInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
}
