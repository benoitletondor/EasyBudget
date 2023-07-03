package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import kotlinx.coroutines.flow.Flow

interface Accounts {
    fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>>
}