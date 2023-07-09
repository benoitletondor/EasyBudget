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

package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PurchaseFlowResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(private val iab: Iab) : ViewModel() {
    private val premiumFlowStatusMutableFlow = MutableStateFlow(PremiumFlowStatus.NOT_STARTED)
    val premiumFlowStatusFlow: Flow<PremiumFlowStatus> = premiumFlowStatusMutableFlow

    private val premiumFlowErrorEventMutableFlow = MutableLiveFlow<PurchaseFlowResult>()
    val premiumFlowErrorEventFlow: Flow<PurchaseFlowResult> = premiumFlowErrorEventMutableFlow

    fun onBuyPremiumClicked(activity: Activity) {
        premiumFlowStatusMutableFlow.value = PremiumFlowStatus.LOADING

        viewModelScope.launch {
            when(val result = iab.launchPremiumSubscriptionFlow(activity)) {
                PurchaseFlowResult.Cancelled -> {
                    premiumFlowErrorEventMutableFlow.emit(result)
                    premiumFlowStatusMutableFlow.value = PremiumFlowStatus.NOT_STARTED
                }
                PurchaseFlowResult.Success -> {
                    premiumFlowErrorEventMutableFlow.emit(result)
                    premiumFlowStatusMutableFlow.value = PremiumFlowStatus.DONE
                }
                is PurchaseFlowResult.Error -> {
                    Logger.error("Error while launching premium purchase flow: ${result.reason}")
                    premiumFlowErrorEventMutableFlow.emit(result)
                    premiumFlowStatusMutableFlow.value = PremiumFlowStatus.NOT_STARTED
                }
            }
        }
    }
}

enum class PremiumFlowStatus {
    NOT_STARTED,
    LOADING,
    DONE
}