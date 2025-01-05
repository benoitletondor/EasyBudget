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

package com.benoitletondor.easybudgetapp.auth

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import kotlinx.coroutines.flow.StateFlow

interface Auth {
    val state: StateFlow<AuthState>

    fun startAuthentication(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>)
    fun handleActivityResult(resultCode: Int, data: Intent?)
    fun logout()
    suspend fun refreshUserTokens()
}

sealed class AuthState {
    data object NotAuthenticated : AuthState()
    data object Authenticating : AuthState()
    data class Authenticated(val currentUser: CurrentUser) : AuthState()
}

data class CurrentUser(
    val id: String,
    val email: String,
    val token: String,
)