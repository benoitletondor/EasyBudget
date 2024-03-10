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

package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.iab.Pricing
import com.benoitletondor.easybudgetapp.iab.PurchaseFlowResult
import com.benoitletondor.easybudgetapp.iab.PurchaseType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val iab: Iab,
) : ViewModel() {
    private val errorRetryMutableSharedFlow = MutableSharedFlow<Unit>()

    private val eventMutableSharedFlow = MutableSharedFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableSharedFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val userSubscriptionStatus: Flow<SubscriptionStatus> = flow { emit(iab.fetchPricingOrDefault()) }
        .flatMapLatest { pricing ->
            iab.iabStatusFlow
                .map { iabStatus ->
                    return@map when(iabStatus) {
                        PremiumCheckStatus.INITIALIZING, PremiumCheckStatus.CHECKING -> SubscriptionStatus.Verifying
                        PremiumCheckStatus.ERROR -> SubscriptionStatus.Error
                        PremiumCheckStatus.NOT_PREMIUM -> SubscriptionStatus.NotSubscribed(pricing)
                        PremiumCheckStatus.LEGACY_PREMIUM,
                        PremiumCheckStatus.PREMIUM_SUBSCRIBED -> SubscriptionStatus.PremiumSubscribed(pricing)
                        PremiumCheckStatus.PRO_SUBSCRIBED -> SubscriptionStatus.ProSubscribed(pricing)
                    }
                } }
        .retryWhen { cause, _ ->
            Logger.error("Error while fetching subscription pricing", cause)
            emit(SubscriptionStatus.Error)

            errorRetryMutableSharedFlow.first()

            true
        }

    private val premiumPurchaseStatusMutableFlow = MutableStateFlow(PurchaseFlowStatus.NOT_STARTED)
    val premiumPurchaseStatusFlow: Flow<PurchaseFlowStatus> = premiumPurchaseStatusMutableFlow

    private val premiumPurchaseEventMutableFlow = MutableLiveFlow<PurchaseFlowResult>()
    val premiumPurchaseEventFlow: Flow<PurchaseFlowResult> = premiumPurchaseEventMutableFlow

    private val proPurchaseStatusMutableFlow = MutableStateFlow(PurchaseFlowStatus.NOT_STARTED)
    val proPurchaseStatusFlow: Flow<PurchaseFlowStatus> = proPurchaseStatusMutableFlow

    private val proPurchaseEventMutableFlow = MutableLiveFlow<PurchaseFlowResult>()
    val proPurchaseEventFlow: Flow<PurchaseFlowResult> = proPurchaseEventMutableFlow

    fun onRetryButtonPressed() {
        viewModelScope.launch {
            errorRetryMutableSharedFlow.emit(Unit)
        }
    }

    fun onCloseButtonPressed() {
        viewModelScope.launch {
            eventMutableSharedFlow.emit(Event.Finish)
        }
    }

    fun onBuyPremiumClicked(activity: Activity) {
        premiumPurchaseStatusMutableFlow.value = PurchaseFlowStatus.LOADING

        viewModelScope.launch {
            when(val result = iab.launchPremiumSubscriptionFlow(activity)) {
                PurchaseFlowResult.Cancelled -> {
                    premiumPurchaseEventMutableFlow.emit(result)
                    premiumPurchaseStatusMutableFlow.value = PurchaseFlowStatus.NOT_STARTED
                }
                is PurchaseFlowResult.Success -> {
                    if (result.purchaseType == PurchaseType.PREMIUM_SUBSCRIPTION) {
                        premiumPurchaseEventMutableFlow.emit(result)
                        premiumPurchaseStatusMutableFlow.value = PurchaseFlowStatus.DONE
                    }
                }
                is PurchaseFlowResult.Error -> {
                    Logger.error("Error while launching premium purchase flow: ${result.reason}")
                    premiumPurchaseEventMutableFlow.emit(result)
                    premiumPurchaseStatusMutableFlow.value = PurchaseFlowStatus.NOT_STARTED
                }
            }
        }
    }

    fun onBuyProClicked(activity: Activity) {
        premiumPurchaseStatusMutableFlow.value = PurchaseFlowStatus.LOADING

        viewModelScope.launch {
            when(val result = iab.launchProSubscriptionFlow(activity)) {
                PurchaseFlowResult.Cancelled -> {
                    proPurchaseEventMutableFlow.emit(result)
                    proPurchaseStatusMutableFlow.value = PurchaseFlowStatus.NOT_STARTED
                }
                is PurchaseFlowResult.Success -> {
                    if (result.purchaseType == PurchaseType.PRO_SUBSCRIPTION) {
                        proPurchaseEventMutableFlow.emit(result)
                        proPurchaseStatusMutableFlow.value = PurchaseFlowStatus.DONE
                    }
                }
                is PurchaseFlowResult.Error -> {
                    Logger.error("Error while launching pro purchase flow: ${result.reason}")
                    proPurchaseEventMutableFlow.emit(result)
                    proPurchaseStatusMutableFlow.value = PurchaseFlowStatus.NOT_STARTED
                }
            }
        }
    }

    enum class PurchaseFlowStatus {
        NOT_STARTED,
        LOADING,
        DONE
    }

    sealed class SubscriptionStatus {
        data object Verifying : SubscriptionStatus()
        data object Error : SubscriptionStatus()
        data class NotSubscribed(override val pricing: Pricing) : SubscriptionStatus(), WithPricing
        data class PremiumSubscribed(override val pricing: Pricing) : SubscriptionStatus(), WithPricing
        data class ProSubscribed(override val pricing: Pricing) : SubscriptionStatus(), WithPricing
    }

    sealed interface WithPricing {
        val pricing: Pricing
    }

    sealed class Event {
        data object Finish : Event()
    }
}


