/*
 *   Copyright 2020 Benoit LETONDOR
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
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.parameters.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import org.koin.java.KoinJavaComponent.get
import java.lang.RuntimeException

class BackupSettingsViewModel(private val auth: Auth,
                              private val parameters: Parameters,
                              private val appContext: Context) : ViewModel() {

    val cloudBackupStateStream: MutableLiveData<BackupCloudStorageState> = MutableLiveData()
    val authenticationConfirmationDisplayEvent = SingleLiveEvent<Unit>()
    val backupNowErrorEvent = SingleLiveEvent<Throwable>()
    val restorationErrorEvent = SingleLiveEvent<Throwable>()
    val previousBackupAvailableEvent = SingleLiveEvent<Date>()
    val appRestartEvent = SingleLiveEvent<Unit>()
    val restoreConfirmationDisplayEvent = SingleLiveEvent<Date>()
    val deleteConfirmationDisplayEvent = SingleLiveEvent<Unit>()
    val backupDeletionErrorEvent = SingleLiveEvent<Throwable>()
    
    private var backupInProgress = false
    private var restorationInProgress = false
    private var deletionInProgress = false

    private val backupJobObserver = Observer<List<WorkInfo>> {
        cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)
    }
    private val authStateObserver = Observer<AuthState> { authState ->
        if( authState is AuthState.Authenticated ) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        if( parameters.getLastBackupDate() == null ) {
                            getBackupDBMetaData(get(CloudStorage::class.java), auth)?.let {
                                parameters.saveLastBackupDate(it.lastUpdateDate)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(
                            "BackupSettingsViewModel",
                            "Error getting last backup date",
                            e
                        )
                    }

                    null // ?! not sure why it's needed
                }

                cloudBackupStateStream.value = computeBackupCloudStorageState(authState)
            }
        } else {
            cloudBackupStateStream.value = computeBackupCloudStorageState(authState)
        }
    }

    init {
        auth.state.observeForever(authStateObserver)
        getBackupJobInfosLiveData(appContext).observeForever(backupJobObserver)
    }

    override fun onCleared() {
        auth.state.removeObserver(authStateObserver)
        getBackupJobInfosLiveData(appContext).removeObserver(backupJobObserver)

        super.onCleared()
    }

    fun onAuthenticateButtonPressed() {
        authenticationConfirmationDisplayEvent.value = Unit
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
            cloudBackupStateStream.value = newBackupState

            if( newBackupState is BackupCloudStorageState.Activated ) {
                val lastBackupDate = newBackupState.lastBackupDate
                if( lastBackupDate != null ) {
                    previousBackupAvailableEvent.value = lastBackupDate
                }
            }

            scheduleBackup(appContext)
        }
    }

    fun onBackupDeactivated() {
        if( parameters.isBackupEnabled() ) {
            parameters.setBackupEnabled(false)
            cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)

            unscheduleBackup(appContext)
        }
    }

    fun onBackupNowButtonPressed() {
        viewModelScope.launch {
            backupInProgress = true
            cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    val result = backupDB(
                        appContext,
                        get(DB::class.java),
                        get(CloudStorage::class.java),
                        auth,
                        parameters,
                        get(Iab::class.java)
                    )

                    if( result !is ListenableWorker.Result.Success ) {
                        throw RuntimeException(result.toString())
                    }
                }
            } catch (error: Throwable) {
                Log.e("BackupSettingsViewModel", "Error while backup now", error)
                backupNowErrorEvent.value = error
            } finally {
                backupInProgress = false
                cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)
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
        deleteConfirmationDisplayEvent.value = Unit
    }

    fun onDeleteBackupConfirmationConfirmed() {
        viewModelScope.launch {
            deletionInProgress = true
            cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    deleteBackup(auth, get(CloudStorage::class.java), get(Iab::class.java))
                    parameters.saveLastBackupDate(null)
                }
            } catch (error: Throwable) {
                Log.e("BackupSettingsViewModel", "Error while deleting backup", error)
                backupDeletionErrorEvent.value = error
            } finally {
                deletionInProgress = false
                cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)
            }
        }
    }

    fun onDeleteBackupConfirmationCancelled() {
        // No-op
    }

    private fun startRestoreFlow() {
        val lastBackupDate = (cloudBackupStateStream.value as? BackupCloudStorageState.Activated)?.lastBackupDate
        if( lastBackupDate == null ) {
            Logger.error("Starting restore with no last backup date")
            return
        }

        restoreConfirmationDisplayEvent.value = lastBackupDate
    }

    private fun restoreData() {
        viewModelScope.launch {
            restorationInProgress = true
            cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)

            try {
                withContext(Dispatchers.IO) {
                    restoreLatestDBBackup(appContext, auth, get(CloudStorage::class.java), get(Iab::class.java))
                }

                appRestartEvent.postValue(Unit)
            } catch (error: Throwable) {
                Log.e("BackupSettingsViewModel", "Error while restoring", error)
                restorationErrorEvent.value = error
            } finally {
                restorationInProgress = false
                cloudBackupStateStream.value = computeBackupCloudStorageState(auth.state.value)
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
    object NotAuthenticated : BackupCloudStorageState()
    object Authenticating : BackupCloudStorageState()
    data class NotActivated(val currentUser: CurrentUser) : BackupCloudStorageState()
    data class Activated(val currentUser: CurrentUser,
                         val lastBackupDate: Date?,
                         val backupNowAvailable: Boolean,
                         val restoreAvailable: Boolean): BackupCloudStorageState()
    data class BackupInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
    data class RestorationInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
    data class DeletionInProgress(val currentUser: CurrentUser): BackupCloudStorageState()
}
