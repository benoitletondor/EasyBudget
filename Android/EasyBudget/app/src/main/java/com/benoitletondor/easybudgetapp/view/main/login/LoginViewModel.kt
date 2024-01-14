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

package com.benoitletondor.easybudgetapp.view.main.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: Auth,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val shouldDismissAfterAuth = savedStateHandle.get<Boolean>(LoginActivity.SHOULD_DISMISS_AFTER_AUTH_EXTRA)
        ?: throw IllegalStateException("Missing SHOULD_DISMISS_AFTER_AUTH_EXTRA extra")

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    val stateFlow: StateFlow<State> = auth.state
        .map {
            when(it) {
                is AuthState.Authenticated -> State.Authenticated(it.currentUser)
                AuthState.Authenticating -> State.Loading
                AuthState.NotAuthenticated -> State.NotAuthenticated
            }
        }
        .onEach {
            if (it is State.Authenticated && shouldDismissAfterAuth) {
                eventMutableFlow.emit(Event.Finish)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        auth.handleActivityResult(requestCode, resultCode, data)
    }

    fun onAuthenticatedButtonClicked(activity: Activity) {
        auth.startAuthentication(activity)
    }

    fun onLogoutButtonClicked() {
        auth.logout()
    }

    fun onFinishButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.Finish)
        }
    }

    sealed class State {
        data object Loading : State()
        data object NotAuthenticated : State()
        data class Authenticated(val user: CurrentUser) : State()
    }

    sealed class Event {
        data object Finish : Event()
    }
}