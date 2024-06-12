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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val iab: Iab,
) : ViewModel() {
    private val errorRetryMutableSharedFlow = MutableSharedFlow<Unit>()

    private val eventMutableSharedFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableSharedFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val userSubscriptionStatusFlow: StateFlow<SubscriptionStatus> = flow { emit(iab.fetchPricingOrDefault()) }
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, SubscriptionStatus.Verifying)

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

    fun onCancelButtonClicked() {
        viewModelScope.launch {
            eventMutableSharedFlow.emit(Event.Finish)
        }
    }

    fun onBuyPremiumClicked(activity: Activity) {
        viewModelScope.launch {
            val result = iab.launchPremiumSubscriptionFlow(activity)
            eventMutableSharedFlow.emit(Event.PremiumPurchaseResult(result))

            if (result is PurchaseFlowResult.Success) {
                eventMutableSharedFlow.emit(Event.Finish)
            }
        }
    }

    fun onBuyProClicked(activity: Activity) {
        viewModelScope.launch {
            val result = iab.launchProSubscriptionFlow(activity)
            eventMutableSharedFlow.emit(Event.ProPurchaseResult(result))

            if (result is PurchaseFlowResult.Success) {
                eventMutableSharedFlow.emit(Event.Finish)
            }
        }
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
        data class PremiumPurchaseResult(val result: PurchaseFlowResult) : Event()
        data class ProPurchaseResult(val result: PurchaseFlowResult) : Event()
    }
}


