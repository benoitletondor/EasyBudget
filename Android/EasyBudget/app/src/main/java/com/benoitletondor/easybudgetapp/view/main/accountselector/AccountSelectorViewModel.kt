package com.benoitletondor.easybudgetapp.view.main.accountselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.getLatestSelectedOnlineAccountId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSelectorViewModel @Inject constructor(
    private val iab: Iab,
    private val auth: Auth,
    private val accounts: Accounts,
    private val parameters: Parameters,
) : ViewModel() {
    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

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
            }
    ) { iabStatus, authStatus, onlineAccounts ->
        return@combine when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> State.Loading
            PremiumCheckStatus.ERROR -> State.IabError
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> State.NotPro
            PremiumCheckStatus.PRO_SUBSCRIBED -> when(authStatus) {
                is AuthState.Authenticated -> {
                    val ownAccounts = onlineAccounts
                        .filter { it.isUserOwner }
                        .map { Account(
                            id = it.id,
                            secret = it.secret,
                            name = it.name,
                            ownerEmail = it.ownerEmail,
                            selected = parameters.getLatestSelectedOnlineAccountId() == it.id,
                        ) }

                    val invitedAccounts = onlineAccounts
                        .filter { !it.isUserOwner }
                        .map { Account(
                            id = it.id,
                            secret = it.secret,
                            name = it.name,
                            ownerEmail = it.ownerEmail,
                            selected = parameters.getLatestSelectedOnlineAccountId() == it.id,
                        ) }

                    State.AccountsAvailable(
                        isOfflineSelected = parameters.getLatestSelectedOnlineAccountId() == null ||
                            (ownAccounts.none { it.selected } && invitedAccounts.none { it.selected } ),
                        ownAccounts = ownAccounts,
                        invitedAccounts = invitedAccounts,
                    )
                }
                AuthState.Authenticating -> State.Loading
                AuthState.NotAuthenticated -> State.NotAuthenticated
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onIabErrorRetryButtonClicked() {
        iab.updateIAPStatusIfNeeded()
    }

    fun onAccountSelected(account: MainViewModel.SelectedAccount.Selected) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.AccountSelected(account))
        }
    }

    fun onBecomeProButtonClicked() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenProScreen)
        }
    }

    fun onLoginButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenLoginScreen)
        }
    }

    data class Account(
        val id: String,
        val secret: String,
        val name: String,
        val selected: Boolean,
        val ownerEmail: String,
    )

    sealed class State {
        object Loading : State()
        object IabError : State()
        object NotPro : State()
        object NotAuthenticated : State()
        data class AccountsAvailable(
            val isOfflineSelected: Boolean,
            val ownAccounts: List<Account>,
            val invitedAccounts: List<Account>,
        ) : State()
    }

    sealed class Event {
        data class AccountSelected(val account: MainViewModel.SelectedAccount.Selected) : Event()
        object OpenProScreen : Event()
        object OpenLoginScreen : Event()
    }
}