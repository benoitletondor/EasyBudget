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

package com.benoitletondor.easybudgetapp.view.main.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    auth: Auth,
    private val accounts: Accounts,
) : ViewModel() {
    private var lastSubmittedValue: String? = null
    private val isCreatingMutableFlow = MutableStateFlow(false)

    val stateFlow: StateFlow<State> = combine(auth.state, isCreatingMutableFlow) { authState, isCreating ->
        if (isCreating && authState is AuthState.Authenticated) {
            return@combine State.Creating(authState.currentUser)
        }

        return@combine when(authState) {
            is AuthState.Authenticated -> State.Ready(
                initialNameValue = lastSubmittedValue ?: "",
                currentUser = authState.currentUser,
            )
            AuthState.Authenticating -> State.Loading
            AuthState.NotAuthenticated -> State.NotAuthenticatedError
        }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    fun onCreateAccountButtonPressed(accountName: String) {
        val readyState = stateFlow.value as? State.Ready ?: return
        lastSubmittedValue = accountName
        isCreatingMutableFlow.value = true

        viewModelScope.launch {
            try {
                if (accountName.length > 50) {
                    throw IllegalStateException("Account name cannot be more than 50 chars.")
                }

                accounts.createAccount(
                    currentUser = readyState.currentUser,
                    name = accountName,
                )

                eventMutableFlow.emit(Event.SuccessCreatingAccount)
                eventMutableFlow.emit(Event.Finish)
            } catch (e: Exception) {
                if (e is CancellationException) { throw e }

                isCreatingMutableFlow.value = false
                eventMutableFlow.emit(Event.ErrorWhileCreatingAccount(e))
            }
        }
    }

    fun onFinishButtonClicked() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.Finish)
        }
    }

    sealed class State {
        data object Loading : State()
        data class Ready(val initialNameValue: String, val currentUser: CurrentUser) : State()
        data object NotAuthenticatedError : State()
        data class Creating(val currentUser: CurrentUser) : State()
    }

    sealed class Event {
        class ErrorWhileCreatingAccount(val error: Throwable) : Event()
        data object SuccessCreatingAccount : Event()
        data object Finish : Event()
    }
}