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

package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.accounts.model.AccountCredentials
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import kotlinx.coroutines.flow.Flow

interface Accounts {
    fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>>
    fun watchAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials): Flow<Account>
    fun watchInvitationsForAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials): Flow<List<Invitation>>
    fun watchHasPendingInvitedAccounts(currentUser: CurrentUser): Flow<Boolean>
    fun watchPendingInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>>
    suspend fun sendInvitationToAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials, invitedUserEmail: String)
    suspend fun acceptInvitationToAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials)
    suspend fun rejectInvitationToAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials)
    suspend fun createAccount(currentUser: CurrentUser, name: String): Account
    suspend fun deleteInvitation(currentUser: CurrentUser, invitation: Invitation)
    suspend fun updateAccountName(currentUser: CurrentUser, accountCredentials: AccountCredentials, newName: String)
    suspend fun leaveAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials)
    suspend fun deleteAccount(currentUser: CurrentUser, accountCredentials: AccountCredentials)
}