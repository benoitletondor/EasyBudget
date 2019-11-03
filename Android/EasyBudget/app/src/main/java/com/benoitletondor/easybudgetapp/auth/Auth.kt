package com.benoitletondor.easybudgetapp.auth

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData

interface Auth {
    val state: LiveData<AuthState>

    fun startAuthentication(activity: Activity)
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}

sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val currentUser: CurrentUser) : AuthState()
}

interface CurrentUser {
    val id: String
    val email: String
}