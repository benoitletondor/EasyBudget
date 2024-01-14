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

package com.benoitletondor.easybudgetapp.view.main

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.accounts.model.AccountCredentials
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getLatestSelectedOnlineAccountId
import com.benoitletondor.easybudgetapp.parameters.setLatestSelectedOnlineAccountId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iab: Iab,
    private val parameters: Parameters,
    private val accounts: Accounts,
    private val auth: Auth,
) : ViewModel() {
    val premiumStatusFlow: StateFlow<PremiumCheckStatus> = iab.iabStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PremiumCheckStatus.INITIALIZING)

    private val openPremiumEventMutableFlow = MutableLiveFlow<Unit>()
    val openPremiumEventFlow: Flow<Unit> = openPremiumEventMutableFlow

    private val selectedOnlineAccountIdMutableStateFlow: MutableStateFlow<String?>
        = MutableStateFlow(parameters.getLatestSelectedOnlineAccountId())

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    val hasPendingInvitationsFlow: StateFlow<Boolean> = iab.iabStatusFlow.flatMapLatest { iabStatus ->
        when(iabStatus) {
            PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                .flatMapLatest { authState ->
                    when(authState) {
                        is AuthState.Authenticated -> accounts.watchHasPendingInvitedAccounts(authState.currentUser)
                        AuthState.Authenticating,
                        AuthState.NotAuthenticated -> flowOf(false)
                    }
                }
            else -> flowOf(false)
        }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val retryEventMutableFlow = MutableSharedFlow<Unit>()

    val accountSelectionFlow: StateFlow<SelectedAccount> = combine(
        selectedOnlineAccountIdMutableStateFlow,
        iab.iabStatusFlow,
        iab.iabStatusFlow
            .flatMapLatest { iabStatus ->
                when(iabStatus) {
                    PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                        .flatMapLatest { authState ->
                            when(authState) {
                                is AuthState.Authenticated -> accounts.watchAccounts(authState.currentUser)
                                    .map { OnlineAccountResponse.Available(it) }
                                AuthState.Authenticating,
                                AuthState.NotAuthenticated -> flowOf(OnlineAccountResponse.Loading)
                            }
                        }
                    else -> flowOf(OnlineAccountResponse.Loading)
                }
            }
    ) { selectedOnlineAccountId, iabStatus, onlineAccountsResponse ->
        if (selectedOnlineAccountId == null) {
            return@combine SelectedAccount.Selected.Offline
        }

        return@combine when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> SelectedAccount.Loading
            PremiumCheckStatus.ERROR -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.PRO_SUBSCRIBED -> when(onlineAccountsResponse) {
                is OnlineAccountResponse.Available -> onlineAccountsResponse.accounts
                    .firstOrNull { it.id == selectedOnlineAccountId }
                    ?.let { account ->
                        SelectedAccount.Selected.Online(
                            name = account.name,
                            isOwner = account.isUserOwner,
                            ownerEmail = account.ownerEmail,
                            accountId = account.id,
                            accountSecret = account.secret,
                        )
                    } ?: SelectedAccount.Selected.Offline
                OnlineAccountResponse.Loading -> SelectedAccount.Loading
            }
        }
    }.retryWhen { cause, _ ->
        Logger.error("Error while building accountSelectionFlow", cause)
        emit(SelectedAccount.Selected.Offline)

        retryEventMutableFlow.first()

        true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectedAccount.Loading)

    fun onBecomePremiumButtonPressed() {
        viewModelScope.launch {
            openPremiumEventMutableFlow.emit(Unit)
        }
    }

    fun onWelcomeScreenFinished() {
        // No-op
    }

    fun onAccountTapped() {
        viewModelScope.launch {
            retryEventMutableFlow.emit(Unit)
            eventMutableFlow.emit(Event.ShowAccountSelect)
        }
    }

    fun onAccountSelected(account: SelectedAccount.Selected) {
        viewModelScope.launch {
            retryEventMutableFlow.emit(Unit)
        }

        val onlineAccountId = when(account) {
            SelectedAccount.Selected.Offline -> null
            is SelectedAccount.Selected.Online -> account.accountId
        }

        parameters.setLatestSelectedOnlineAccountId(onlineAccountId)
        selectedOnlineAccountIdMutableStateFlow.value = onlineAccountId
    }

    fun shouldShowMenuButtons(): Boolean = iab.isIabReady() && accountSelectionFlow.value is SelectedAccount.Selected

    fun showPremiumMenuButtons(): Boolean = when(iab.iabStatusFlow.value) {
        PremiumCheckStatus.INITIALIZING,
        PremiumCheckStatus.CHECKING,
        PremiumCheckStatus.ERROR,
        PremiumCheckStatus.NOT_PREMIUM -> false
        PremiumCheckStatus.LEGACY_PREMIUM,
        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
        PremiumCheckStatus.PRO_SUBSCRIBED -> true
    }

    private sealed class OnlineAccountResponse {
        data object Loading : OnlineAccountResponse()
        data class Available(val accounts: List<Account>) : OnlineAccountResponse()
    }

    sealed class SelectedAccount {
        data object Loading : SelectedAccount()
        sealed class Selected : SelectedAccount(), Parcelable {
            @Parcelize
            object Offline : Selected()
            @Parcelize
            data class Online(
                val name: String,
                val isOwner: Boolean,
                val ownerEmail: String,
                val accountId: String,
                val accountSecret: String,
            ) : Selected() {
                fun toAccountCredentials() = AccountCredentials(
                    id = accountId,
                    secret = accountSecret,
                )
            }
        }
    }

    sealed class Event {
        data object ShowAccountSelect : Event()
    }
}