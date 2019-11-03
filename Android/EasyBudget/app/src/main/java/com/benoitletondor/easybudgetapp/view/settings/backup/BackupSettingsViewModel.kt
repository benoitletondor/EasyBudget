package com.benoitletondor.easybudgetapp.view.settings.backup

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser

class BackupSettingsViewModel(private val auth: Auth) : ViewModel(), Observer<AuthState> {
    val cloudBackupStateStream: MutableLiveData<BackupCloudStorageState> = MutableLiveData()

    init {
        auth.state.observeForever(this)
    }

    override fun onCleared() {
        auth.state.removeObserver(this)

        super.onCleared()
    }

    override fun onChanged(authState: AuthState?) {
        cloudBackupStateStream.value = getBackupCloudStorageState(authState)
    }

    private fun getBackupCloudStorageState(authState: AuthState?): BackupCloudStorageState {
        return when(authState) {
            AuthState.NotAuthenticated -> BackupCloudStorageState.NotAuthenticated
            AuthState.Authenticating -> BackupCloudStorageState.Authenticating
            is AuthState.Authenticated -> BackupCloudStorageState.NotActivated(authState.currentUser)
            null -> BackupCloudStorageState.NotAuthenticated
        }
    }

    fun onAuthenticateButtonPressed(activity: Activity) {
        auth.startAuthentication(activity)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        auth.handleActivityResult(requestCode, resultCode, data)
    }
}

sealed class BackupCloudStorageState {
    object NotAuthenticated : BackupCloudStorageState()
    object Authenticating : BackupCloudStorageState()
    data class NotActivated(val currentUser: CurrentUser) : BackupCloudStorageState()
}