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

package com.benoitletondor.easybudgetapp.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseUser

private const val SIGN_IN_REQUEST_CODE = 10524

class FirebaseAuth(private val auth: com.google.firebase.auth.FirebaseAuth) : Auth {

    private val currentState = MutableLiveData(getAuthState())

    override val state: LiveData<AuthState>
        get() = currentState

    init {
        auth.addAuthStateListener {
            currentState.postValue(getAuthState())
        }
    }

    override fun startAuthentication(activity: Activity) {
        currentState.postValue(AuthState.Authenticating)

        try {
            activity.startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                    .build(),
                SIGN_IN_REQUEST_CODE
            )
        } catch (error: Throwable) {
            Log.e("FirebaseAuth", "Error launching auth activity", error)
            currentState.postValue(getAuthState())
        }

    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SIGN_IN_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                val response = IdpResponse.fromResultIntent(data)
                if( response != null ) {
                    Log.e(
                        "FirebaseAuth",
                        "Error while authenticating: ${response.error?.errorCode}: ${response.error?.localizedMessage}",
                        response.error
                    )
                }
            }

            currentState.postValue(getAuthState())
        }
    }

    override fun logout() {
        auth.signOut()
        currentState.postValue(getAuthState())
    }

    private fun getAuthState(): AuthState {
        val currentUser = auth.currentUser
        return if( currentUser == null ) {
            AuthState.NotAuthenticated
        } else {
            AuthState.Authenticated(FirebaseCurrentUser(currentUser))
        }
    }
}

class FirebaseCurrentUser(private val user: FirebaseUser) : CurrentUser {
    override val email: String
        get() = user.email!!

    override val id: String
        get() = user.uid
}