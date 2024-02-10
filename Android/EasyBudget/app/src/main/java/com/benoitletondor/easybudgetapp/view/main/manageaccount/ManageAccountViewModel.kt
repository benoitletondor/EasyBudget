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

package com.benoitletondor.easybudgetapp.view.main.manageaccount

import android.util.Patterns
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.accounts.model.InvitationStatus
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.benoitletondor.easybudgetapp.view.main.manageaccount.ManageAccountActivity.Companion.SELECTED_ACCOUNT_EXTRA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageAccountViewModel @Inject constructor(
    auth: Auth,
    private val accounts: Accounts,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val selectedAccount = savedStateHandle.get<MainViewModel.SelectedAccount.Selected.Online>(SELECTED_ACCOUNT_EXTRA)
        ?: throw IllegalStateException("Missing SELECTED_ACCOUNT_EXTRA arg")

    private val isUpdatingNameMutableFlow = MutableStateFlow(false)
    private val isDeletingInvitationMutableFlow = MutableStateFlow(false)
    private val isSendingInvitationMutableFlow = MutableStateFlow(false)
    private val isLeavingAccountMutableFlow = MutableStateFlow(false)
    private val isDeletingAccountMutableFlow = MutableStateFlow(false)

    private val retryLoadingMutableFlow = MutableSharedFlow<Unit>()

    val stateFlow: StateFlow<State> = combine(
        auth.state,
        isUpdatingNameMutableFlow,
        isDeletingInvitationMutableFlow,
        isSendingInvitationMutableFlow,
        isLeavingAccountMutableFlow,
        isDeletingAccountMutableFlow,
        auth.state.flatMapLatest { authState ->
            when(authState) {
                is AuthState.Authenticated -> accounts.watchAccount(
                    authState.currentUser,
                    selectedAccount.toAccountCredentials(),
                ).map { AccountLoadingState.Loaded(it) }
                AuthState.Authenticating -> flowOf(AccountLoadingState.Loading())
                AuthState.NotAuthenticated -> flowOf(AccountLoadingState.Loading())
            }
        },
        if (selectedAccount.isOwner) {
            auth.state.flatMapLatest { authState ->
                when(authState) {
                    is AuthState.Authenticated -> accounts.watchInvitationsForAccount(authState.currentUser, selectedAccount.toAccountCredentials())
                        .map { AccountLoadingState.Loaded(it) }
                    AuthState.Authenticating -> flowOf(AccountLoadingState.Loading())
                    AuthState.NotAuthenticated -> flowOf(AccountLoadingState.Loading())
                }
            }
        } else {
            flowOf(AccountLoadingState.Loaded(emptyList()))
        },
    ) { authState,
        isUpdatingName,
        isDeletingInvitation,
        isSendingInvitation,
        isLeavingAccount,
        isDeletingAccount,
        accountLoadingState,
        invitationsLoadingState ->

        if (isUpdatingName) {
            return@combine State.Updating
        }

        if (isDeletingInvitation) {
            return@combine State.DeletingInvitation
        }

        if (isSendingInvitation) {
            return@combine State.SendingInvitation
        }

        if (isLeavingAccount) {
            return@combine State.LeavingAccount
        }

        if (isDeletingAccount) {
            return@combine State.DeletingAccount
        }

        val account = when(accountLoadingState) {
            is AccountLoadingState.Loaded -> accountLoadingState.data
            is AccountLoadingState.Loading -> return@combine State.Loading
        }

        val invitations = when(invitationsLoadingState) {
            is AccountLoadingState.Loaded -> invitationsLoadingState.data
            is AccountLoadingState.Loading -> return@combine State.Loading
        }

        return@combine when(authState) {
            is AuthState.Authenticated -> if (selectedAccount.isOwner) {
                State.Ready.Owner(
                    accountName = account.name,
                    currentUser = authState.currentUser,
                    invitationsSent = invitations
                        .filter {
                            it.status == InvitationStatus.SENT
                        },
                    invitationsAccepted =  invitations
                        .filter {
                            it.status == InvitationStatus.ACCEPTED
                        },
                )
            } else {
                State.Ready.Invited(
                    accountName = account.name,
                    currentUser = authState.currentUser,
                )
            }
            AuthState.Authenticating -> State.Loading
            AuthState.NotAuthenticated -> State.Error(IllegalStateException("Not authenticated"))
        }
    }
    .retryWhen { cause, _ ->
        Logger.error("Error while loading account details", cause)
        emit(State.Error(cause))

        retryLoadingMutableFlow.first()

        true
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    fun onUpdateAccountNameClicked(newName: String) {
        val state = stateFlow.value as? State.Ready.Owner ?: kotlin.run {
            Logger.error("onUpdateAccountNameClicked while not being in loaded state")
            return
        }

        if (newName.trim().isEmpty()) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ErrorUpdatingAccountName(IllegalStateException("Account name must not be empty")))
            }

            return
        }

        if (newName.length >= 50) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ErrorUpdatingAccountName(IllegalStateException("Account name must be less than 50 chars")))
            }

            return
        }

        viewModelScope.launch {
            isUpdatingNameMutableFlow.value = true

            try {
                accounts.updateAccountName(state.currentUser, selectedAccount.toAccountCredentials(), newName)
                eventMutableFlow.emit(Event.AccountNameUpdated)
            } catch (e: Exception) {
                if (e is CancellationException) { return@launch }

                Logger.error("Error while updating account name", e)
                eventMutableFlow.emit(Event.ErrorUpdatingAccountName(e))
            } finally {
                isUpdatingNameMutableFlow.value = false
            }
        }
    }

    fun onInvitationDeleteConfirmed(invitation: Invitation) {
        val state = stateFlow.value as? State.Ready.Owner ?: kotlin.run {
            Logger.error("onInvitationDeleteConfirmed while not being in loaded state")
            return
        }

        viewModelScope.launch {
            isDeletingInvitationMutableFlow.value = true

            try {
                accounts.deleteInvitation(state.currentUser, invitation)
                eventMutableFlow.emit(Event.InvitationDeleted(invitation))
            } catch (e: Exception) {
                if (e is CancellationException) { return@launch }

                Logger.error("Error while deleting invitation", e)
                eventMutableFlow.emit(Event.ErrorDeletingInvitation(e))
            } finally {
                isDeletingInvitationMutableFlow.value = false
            }
        }
    }

    fun onRetryButtonClicked() {
        viewModelScope.launch {
            retryLoadingMutableFlow.emit(Unit)
        }
    }

    fun onLeaveAccountConfirmed() {
        val state = stateFlow.value as? State.Ready.Invited ?: kotlin.run {
            Logger.error("onLeaveAccountConfirmed while not being in loaded state")
            return
        }

        viewModelScope.launch {
            isLeavingAccountMutableFlow.value = true

            try {
                accounts.leaveAccount(state.currentUser, selectedAccount.toAccountCredentials())
                eventMutableFlow.emit(Event.AccountLeft)
                eventMutableFlow.emit(Event.Finish)
            } catch (e: Exception) {
                if (e is CancellationException) { return@launch }

                Logger.error("Error while leaving account", e)
                eventMutableFlow.emit(Event.ErrorWhileLeavingAccount(e))
            } finally {
                isLeavingAccountMutableFlow.value = false
            }
        }
    }

    fun onInviteEmailToAccount(email: String) {
        val state = stateFlow.value as? State.Ready.Owner ?: kotlin.run {
            Logger.error("onInviteEmailToAccount while not being in loaded state")
            return
        }

        if (email.trim().isEmpty()) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ErrorWhileInviting(IllegalStateException("Email must not be empty")))
            }

            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ErrorWhileInviting(IllegalStateException("Email is invalid")))
            }

            return
        }

        if (selectedAccount.ownerEmail.trim() == email.lowercase().trim()) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ErrorWhileInviting(IllegalStateException("Cannot invite account owner")))
            }

            return
        }

        viewModelScope.launch {
            isSendingInvitationMutableFlow.value = true

            try {
                accounts.sendInvitationToAccount(
                    state.currentUser,
                    selectedAccount.toAccountCredentials(),
                    email,
                )
                eventMutableFlow.emit(Event.InvitationSent(email))
            } catch (e: Exception) {
                if (e is CancellationException) { return@launch }

                Logger.error("Error while inviting to account", e)
                eventMutableFlow.emit(Event.ErrorWhileInviting(e))
            } finally {
                isSendingInvitationMutableFlow.value = false
            }
        }
    }

    fun onDeleteAccountConfirmed() {
        val state = stateFlow.value as? State.Ready.Owner ?: kotlin.run {
            Logger.error("onDeleteAccountConfirmed while not being in loaded state")
            return
        }

        viewModelScope.launch {
            isDeletingAccountMutableFlow.value = true

            try {
                val credentials = selectedAccount.toAccountCredentials()
                val onlineDB = (AccountViewModel.getCurrentDB() as? OnlineDB)
                    ?: throw IllegalStateException("No online DB found")

                if (onlineDB.account.id != credentials.id || onlineDB.account.secret != credentials.secret) {
                    throw IllegalStateException("Mismatching accounts")
                }

                onlineDB.deleteAllEntries()
                accounts.deleteAccount(state.currentUser, selectedAccount.toAccountCredentials())

                eventMutableFlow.emit(Event.AccountDeleted)
                eventMutableFlow.emit(Event.Finish)
            } catch (e: Exception) {
                if (e is CancellationException) { return@launch }

                Logger.error("Error while deleting account", e)
                eventMutableFlow.emit(Event.ErrorWhileDeletingAccount(e))
            } finally {
                isDeletingAccountMutableFlow.value = false
            }
        }
    }

    private sealed class AccountLoadingState<T> {
        class Loading<T> : AccountLoadingState<T>()
        data class Loaded<T>(val data: T) : AccountLoadingState<T>()
    }

    sealed class State {
        data object Loading : State()
        sealed class Ready : State() {
            abstract val accountName: String
            abstract val currentUser: CurrentUser

            data class Owner(
                override val accountName: String,
                override val currentUser: CurrentUser,
                val invitationsSent: List<Invitation>,
                val invitationsAccepted: List<Invitation>,
            ) : Ready()

            data class Invited(
                override val accountName: String,
                override val currentUser: CurrentUser,
            ) : Ready()
        }
        data class Error(val error: Throwable) : State()
        data object Updating : State()
        data object DeletingInvitation : State()
        data object SendingInvitation : State()
        data object LeavingAccount : State()
        data object DeletingAccount : State()
    }

    sealed class Event {
        data class ErrorDeletingInvitation(val error: Exception) : Event()
        data class InvitationDeleted(val invitation: Invitation) : Event()
        data class ErrorUpdatingAccountName(val error: Exception) : Event()
        data object AccountNameUpdated : Event()
        data class ErrorWhileLeavingAccount(val error: Exception) : Event()
        data object AccountLeft : Event()
        data object Finish : Event()
        data class ErrorWhileInviting(val error: Exception) : Event()
        data class InvitationSent(val email: String) : Event()
        data object AccountDeleted : Event()
        data class ErrorWhileDeletingAccount(val error: Exception) : Event()
    }
}