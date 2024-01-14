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

package com.benoitletondor.easybudgetapp.auth

import android.app.Activity
import android.content.Intent
import com.benoitletondor.easybudgetapp.helper.Logger
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val SIGN_IN_REQUEST_CODE = 10524

class FirebaseAuth(
    private val auth: com.google.firebase.auth.FirebaseAuth,
) : Auth, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val currentState = MutableStateFlow<AuthState>(AuthState.Authenticating)
    override val state: StateFlow<AuthState> = currentState

    init {
        auth.addAuthStateListener {
            updateAuthState()
        }
    }

    override fun startAuthentication(activity: Activity) {
        currentState.value = AuthState.Authenticating

        try {
            activity.startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                    .build(),
                SIGN_IN_REQUEST_CODE
            )
        } catch (error: Throwable) {
            Logger.error("FirebaseAuth", "Error launching auth activity", error)
            updateAuthState()
        }

    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SIGN_IN_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                val response = IdpResponse.fromResultIntent(data)
                if( response != null ) {
                    Logger.error(
                        "FirebaseAuth",
                        "Error while authenticating: ${response.error?.errorCode}: ${response.error?.localizedMessage}",
                        response.error
                    )
                }
            }

            updateAuthState()
        }
    }

    override fun logout() {
        auth.signOut()
        updateAuthState()
    }

    override suspend fun refreshUserTokens() {
        currentState.value = getAuthState(forceRefreshToken = true)
    }

    private fun updateAuthState() {
        launch {
            currentState.value = getAuthState(forceRefreshToken = false)
        }
    }

    private suspend fun getAuthState(forceRefreshToken: Boolean): AuthState {
        val firebaseUser = auth.currentUser
        return if( firebaseUser == null ) {
            AuthState.NotAuthenticated
        } else {
            try {
                val token = firebaseUser.getIdToken(forceRefreshToken).await().token!!
                AuthState.Authenticated(
                    currentUser = firebaseUser.toCurrentUser(token),
                )
            } catch (e: Exception) {
                Logger.error("Error while getting firebase token", e)
                AuthState.NotAuthenticated
            }
        }
    }
}

private fun FirebaseUser.toCurrentUser(token: String) = CurrentUser(
    id = uid,
    email = email!!,
    token,
)