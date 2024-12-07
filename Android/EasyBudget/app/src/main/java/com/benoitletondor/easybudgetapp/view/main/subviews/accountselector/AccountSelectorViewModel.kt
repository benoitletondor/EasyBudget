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

package com.benoitletondor.easybudgetapp.view.main.subviews.accountselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.AccountCredentials
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.OfflineAccountBackupStatus
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.helper.getOfflineAccountBackupStatusFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.watchIsBackupEnabled
import com.benoitletondor.easybudgetapp.parameters.watchLatestSelectedOnlineAccountId
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
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSelectorViewModel @Inject constructor(
    private val iab: Iab,
    private val auth: Auth,
    private val accounts: Accounts,
    parameters: Parameters,
) : ViewModel() {
    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private val loadingInvitationMutableFlow = MutableStateFlow<Invitation?>(null)

    private val retryMutableFlow = MutableSharedFlow<Unit>()

    val stateFlow: StateFlow<State> = combine(
        iab.iabStatusFlow,
        auth.state,
        iab.iabStatusFlow
            .flatMapLatest { iabStatus ->
                when(iabStatus) {
                    PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                        .flatMapLatest { authState ->
                            when(authState) {
                                is AuthState.Authenticated -> accounts.watchAccounts(authState.currentUser)
                                AuthState.Authenticating,
                                AuthState.NotAuthenticated -> flowOf(emptyList())
                            }
                        }
                    else -> flowOf(emptyList())
                }
            },
        iab.iabStatusFlow
            .flatMapLatest { iabStatus ->
                when(iabStatus) {
                    PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                        .flatMapLatest { authState ->
                            when(authState) {
                                is AuthState.Authenticated -> accounts.watchPendingInvitedAccounts(authState.currentUser)
                                AuthState.Authenticating,
                                AuthState.NotAuthenticated -> flowOf(emptyList())
                            }
                        }
                    else -> flowOf(emptyList())
                }
            },
        loadingInvitationMutableFlow,
        parameters.watchLatestSelectedOnlineAccountId(),
        getOfflineAccountBackupStatusFlow(iab, parameters, auth),
    ) { iabStatus, authStatus, onlineAccounts, pendingAccountsInvitation, maybeLoadingInvitation, maybeSelectedOnlineAccountId, offlineAccountBackupStatus ->
        return@combine when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> State.Loading
            PremiumCheckStatus.ERROR -> State.IabError
            PremiumCheckStatus.NOT_PREMIUM -> State.NotPro(offlineAccountBackupStatus = offlineAccountBackupStatus)
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> State.NotPro(offlineAccountBackupStatus = offlineAccountBackupStatus)
            PremiumCheckStatus.PRO_SUBSCRIBED -> when(authStatus) {
                is AuthState.Authenticated -> {
                    val ownAccounts = onlineAccounts
                        .filter { it.isUserOwner }
                        .map { it.toViewModelAccount(maybeSelectedOnlineAccountId = maybeSelectedOnlineAccountId) }

                    val invitedAccounts = onlineAccounts
                        .filter { !it.isUserOwner }
                        .map { it.toViewModelAccount(maybeSelectedOnlineAccountId = maybeSelectedOnlineAccountId) }

                    State.AccountsAvailable(
                        userEmail = authStatus.currentUser.email,
                        isOfflineSelected = maybeSelectedOnlineAccountId == null ||
                            (ownAccounts.none { it.selected } && invitedAccounts.none { it.selected } ),
                        ownAccounts = ownAccounts.take(5),
                        showCreateOnlineAccountButton = ownAccounts.size < 5,
                        invitedAccounts = invitedAccounts,
                        pendingInvitations = pendingAccountsInvitation.map { account ->
                            Invitation(
                                account = account.toViewModelAccount(maybeSelectedOnlineAccountId = maybeSelectedOnlineAccountId),
                                user = authStatus.currentUser,
                                isLoading = maybeLoadingInvitation?.account?.id == account.id,
                            )
                        },
                        offlineAccountBackupStatus = offlineAccountBackupStatus,
                    )
                }
                AuthState.Authenticating -> State.Loading
                AuthState.NotAuthenticated -> State.NotAuthenticated(
                    offlineAccountBackupStatus = offlineAccountBackupStatus,
                )
            }
        }
    }
    .retryWhen { cause, _ ->
        Logger.error("Error while computing account selector stateFlow", cause)
        emit(State.Error(cause))

        retryMutableFlow.first()

        true
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onIabErrorRetryButtonClicked() {
        iab.updateIAPStatusIfNeeded()
    }

    fun onRetryErrorButtonClicked() {
        viewModelScope.launch {
            retryMutableFlow.emit(Unit)
        }
    }

    fun onAcceptInvitationConfirmed(invitation: Invitation) {
        viewModelScope.launch {
            if (loadingInvitationMutableFlow.value != null) {
                Logger.debug("onAcceptInvitationConfirmed clicked while already accepting, ignoring")
                return@launch
            }

            loadingInvitationMutableFlow.value = invitation

            try {
                accounts.acceptInvitationToAccount(
                    currentUser = invitation.user,
                    accountCredentials = invitation.account.toAccountCredentials(),
                )

                eventMutableFlow.emit(Event.InvitationAccepted)
            } catch (e: Exception) {
                if (e is CancellationException) { throw e }

                Logger.error("Error while accepting invitation", e)
                eventMutableFlow.emit(Event.ErrorAcceptingInvitation(e))
            } finally {
                loadingInvitationMutableFlow.value = null
            }
        }
    }

    fun onRejectInvitationConfirmed(invitation: Invitation) {
        viewModelScope.launch {
            if (loadingInvitationMutableFlow.value != null) {
                Logger.debug("onAcceptInvitationConfirmed clicked while already accepting, ignoring")
                return@launch
            }

            loadingInvitationMutableFlow.value = invitation

            try {
                accounts.rejectInvitationToAccount(
                    currentUser = invitation.user,
                    accountCredentials = invitation.account.toAccountCredentials(),
                )

                eventMutableFlow.emit(Event.InvitationRejected)
            } catch (e: Exception) {
                if (e is CancellationException) { throw e }

                Logger.error("Error while rejecting invitation", e)
                eventMutableFlow.emit(Event.ErrorRejectingInvitation(e))
            } finally {
                loadingInvitationMutableFlow.value = null
            }
        }
    }

    data class Account(
        val id: String,
        val secret: String,
        val name: String,
        val selected: Boolean,
        val ownerEmail: String,
    ) {
        fun toAccountCredentials() = AccountCredentials(
            id = id,
            secret = secret,
        )
    }

    private fun com.benoitletondor.easybudgetapp.accounts.model.Account.toViewModelAccount(maybeSelectedOnlineAccountId: String?) = Account(
        id = id,
        secret = secret,
        name = name,
        ownerEmail = ownerEmail,
        selected = maybeSelectedOnlineAccountId == id,
    )

    data class Invitation(
        val account: Account,
        val user: CurrentUser,
        val isLoading: Boolean,
    )

    sealed interface OfflineAccountBackupStateAvailable {
        val offlineAccountBackupStatus: OfflineAccountBackupStatus
    }

    sealed class State {
        data object Loading : State()
        data object IabError : State()
        data class Error(val cause: Throwable) : State()
        data class NotPro(override val offlineAccountBackupStatus: OfflineAccountBackupStatus) : State(), OfflineAccountBackupStateAvailable
        data class NotAuthenticated(override val offlineAccountBackupStatus: OfflineAccountBackupStatus) : State(), OfflineAccountBackupStateAvailable
        data class AccountsAvailable(
            val userEmail: String,
            val isOfflineSelected: Boolean,
            val ownAccounts: List<Account>,
            val showCreateOnlineAccountButton: Boolean,
            val invitedAccounts: List<Account>,
            val pendingInvitations: List<Invitation>,
            override val offlineAccountBackupStatus: OfflineAccountBackupStatus,
        ) : State(), OfflineAccountBackupStateAvailable
    }

    sealed class Event {
        class ErrorAcceptingInvitation(val error: Throwable) : Event()
        data object InvitationAccepted : Event()
        class ErrorRejectingInvitation(val error: Throwable) : Event()
        data object InvitationRejected : Event()
    }
}