/*
 *   Copyright 2023 Benoit LETONDOR
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
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iab: Iab,
    private val parameters: Parameters,
) : ViewModel() {
    val premiumStatusFlow: StateFlow<PremiumCheckStatus> = iab.iabStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PremiumCheckStatus.INITIALIZING)

    private val openPremiumEventMutableFlow = MutableLiveFlow<Unit>()
    val openPremiumEventFlow: Flow<Unit> = openPremiumEventMutableFlow

    private val selectedAccountMutableStateFlow: MutableStateFlow<SelectedAccount.Selected> = MutableStateFlow(kotlin.run {
        val selectedOnlineAccount = parameters.getLatestSelectedOnlineAccount()
        if (selectedOnlineAccount != null) {
            return@run selectedOnlineAccount
        }

        return@run SelectedAccount.Selected.Offline
    })

    val accountSelectionFlow: StateFlow<SelectedAccount> = combine(
        selectedAccountMutableStateFlow
            .onEach { selectedAccount ->
                when(selectedAccount) {
                    SelectedAccount.Selected.Offline -> parameters.clearLatestSelectedAccount()
                    is SelectedAccount.Selected.Online -> parameters.setLatestSelectedOnlineAccount(selectedAccount)
                }
            },
        iab.iabStatusFlow,
    ) { selectedAccount, iabStatus ->
        if (selectedAccount is SelectedAccount.Selected.Offline) {
            return@combine SelectedAccount.Selected.Offline
        }

        return@combine when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> SelectedAccount.Loading
            PremiumCheckStatus.ERROR -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.PRO_SUBSCRIBED -> selectedAccount
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectedAccount.Loading)

    fun onBecomePremiumButtonPressed() {
        viewModelScope.launch {
            openPremiumEventMutableFlow.emit(Unit)
        }
    }

    fun onWelcomeScreenFinished() {
        // TODO
    }

    fun shouldShowMenuButtons(): Boolean = iab.isIabReady() && accountSelectionFlow.value is SelectedAccount.Selected
    fun showPremiumMenuButtons(): Boolean = iab.isUserPremium()

    sealed class SelectedAccount {
        object Loading : SelectedAccount()
        sealed class Selected : SelectedAccount(), Parcelable {
            @Parcelize
            object Offline : Selected()
            @Parcelize
            data class Online(val accountId: String, val accountSecret: String) : Selected()
        }
    }

    private fun Parameters.getLatestSelectedOnlineAccount(): SelectedAccount.Selected.Online? {
        val id = getString(SELECTED_ACCOUNT_ID_KEY)
        val secret = getString(SELECTED_ACCOUNT_SECRET_KEY)

        if (id != null && secret != null) {
            return SelectedAccount.Selected.Online(id, secret)
        }

        return null
    }

    private fun Parameters.setLatestSelectedOnlineAccount(account: SelectedAccount.Selected.Online) {
        putString(SELECTED_ACCOUNT_ID_KEY, account.accountId)
        putString(SELECTED_ACCOUNT_SECRET_KEY, account.accountSecret)
    }


    private fun Parameters.clearLatestSelectedAccount() {
        remove(SELECTED_ACCOUNT_ID_KEY)
        remove(SELECTED_ACCOUNT_SECRET_KEY)
    }

    companion object {
        private const val SELECTED_ACCOUNT_ID_KEY = "selectedAccountId";
        private const val SELECTED_ACCOUNT_SECRET_KEY = "selectedAccountSecret"
    }
}
