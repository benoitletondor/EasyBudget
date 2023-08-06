package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import kotlinx.coroutines.flow.Flow

interface Accounts {
    fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>>
    fun watchAccount(currentUser: CurrentUser, accountId: String, accountSecret: String): Flow<Account>
    fun watchInvitationsForAccount(currentUser: CurrentUser, accountId: String): Flow<List<Invitation>>
    fun watchHasPendingInvitedAccounts(currentUser: CurrentUser): Flow<Boolean>
    fun watchPendingInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>>
    suspend fun sendInvitationToAccount(currentUser: CurrentUser, account: Account, invitedUserEmail: String)
    suspend fun acceptInvitationToAccount(currentUser: CurrentUser, account: Account)
    suspend fun rejectInvitationToAccount(currentUser: CurrentUser, account: Account)
    suspend fun createAccount(currentUser: CurrentUser, name: String): Account
    suspend fun deleteInvitation(currentUser: CurrentUser, invitation: Invitation)
    suspend fun updateAccountName(currentUser: CurrentUser, accountId: String, newName: String)
    suspend fun leaveAccount(currentUser: CurrentUser, accountId: String)
    suspend fun deleteAccount(currentUser: CurrentUser, accountId: String)
}