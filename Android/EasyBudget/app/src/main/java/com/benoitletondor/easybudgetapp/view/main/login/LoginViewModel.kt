package com.benoitletondor.easybudgetapp.view.main.login

import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: Auth,
) : ViewModel() {
    val stateFlow: StateFlow<State> = auth.state
        .map {
            when(it) {
                is AuthState.Authenticated -> State.Authenticated(it.currentUser)
                AuthState.Authenticating -> State.Loading
                AuthState.NotAuthenticated -> State.NotAuthenticated
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

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
        object Loading : State()
        object NotAuthenticated : State()
        data class Authenticated(val user: CurrentUser) : State()
    }

    sealed class Event {
        object Finish : Event()
    }
}