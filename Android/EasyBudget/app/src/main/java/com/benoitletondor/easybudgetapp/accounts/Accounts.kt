package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import kotlinx.coroutines.flow.Flow

interface Accounts {
    fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>>
    fun watchHasPendingInvitedAccounts(currentUser: CurrentUser): Flow<Boolean>
    fun watchPendingInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>>
    suspend fun sendInvitationToAccount(currentUser: CurrentUser, account: Account, invitedUserEmail: String)
    suspend fun acceptInvitationToAccount(currentUser: CurrentUser, account: Account)
    suspend fun rejectInvitationToAccount(currentUser: CurrentUser, account: Account)
    suspend fun createAccount(currentUser: CurrentUser, name: String): Account
}