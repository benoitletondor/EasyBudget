package com.benoitletondor.easybudgetapp.view.main.manageaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.accounts.model.InvitationStatus
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.manageaccount.ManageAccountActivity.Companion.SELECTED_ACCOUNT_EXTRA
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val retryLoadingMutableFlow = MutableSharedFlow<Unit>()

    val stateFlow: StateFlow<State> = combine(
        auth.state,
        isUpdatingNameMutableFlow,
        isDeletingInvitationMutableFlow,
        isSendingInvitationMutableFlow,
        auth.state.flatMapLatest { authState ->
            when(authState) {
                is AuthState.Authenticated -> accounts.watchAccount(
                    authState.currentUser,
                    selectedAccount.accountId,
                    selectedAccount.accountSecret,
                ).map { AccountLoadingState.Loaded(it) }
                AuthState.Authenticating -> flowOf(AccountLoadingState.Loading())
                AuthState.NotAuthenticated -> flowOf(AccountLoadingState.Loading())
            }
        },
        if (selectedAccount.isOwner) {
            auth.state.flatMapLatest { authState ->
                when(authState) {
                    is AuthState.Authenticated -> accounts.watchInvitationsForAccount(authState.currentUser, selectedAccount.accountId,)
                        .map { AccountLoadingState.Loaded(it) }
                    AuthState.Authenticating -> flowOf(AccountLoadingState.Loading())
                    AuthState.NotAuthenticated -> flowOf(AccountLoadingState.Loading())
                }
            }
        } else {
            flowOf(AccountLoadingState.Loaded(emptyList()))
        },
    ) { authState, isUpdatingName, isDeletingInvitation, isSendingInvitationMutableFlow, accountLoadingState, invitationsLoadingState ->
        if (isUpdatingName) {
            return@combine State.Updating
        }

        if (isDeletingInvitation) {
            return@combine State.DeletingInvitation
        }

        if (isSendingInvitationMutableFlow) {
            return@combine State.SendingInvitation
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
        TODO("Not yet implemented")
    }

    fun onInvitationDeleteConfirmed(invitation: Invitation) {
        TODO("Not yet implemented")
    }

    fun onRetryButtonClicked() {
        viewModelScope.launch {
            retryLoadingMutableFlow.emit(Unit)
        }
    }

    fun onLeaveAccountConfirmed() {
        TODO("Not yet implemented")
    }

    private sealed class AccountLoadingState<T> {
        class Loading<T> : AccountLoadingState<T>()
        data class Loaded<T>(val data: T) : AccountLoadingState<T>()
    }

    sealed class State {
        object Loading : State()
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
        object Updating : State()
        object DeletingInvitation : State()
        object SendingInvitation : State()
        object LeavingAccount : State()
        object DeletingAccount : State()
    }

    sealed class Event {

    }
}