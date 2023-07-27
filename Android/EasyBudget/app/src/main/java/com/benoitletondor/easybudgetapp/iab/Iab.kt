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

package com.benoitletondor.easybudgetapp.iab

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface Iab {
    val iabStatusFlow: StateFlow<PremiumCheckStatus>

    fun isIabReady(): Boolean
    suspend fun isUserPremium(): Boolean
    suspend fun isUserPro(): Boolean
    fun updateIAPStatusIfNeeded()
    suspend fun launchPremiumSubscriptionFlow(activity: Activity): PurchaseFlowResult
    suspend fun launchProSubscriptionFlow(activity: Activity): PurchaseFlowResult
}

sealed class PurchaseFlowResult {
    object Cancelled : PurchaseFlowResult()
    data class Success(val sku: String) : PurchaseFlowResult()
    data class Error(val reason: String): PurchaseFlowResult()
}

/**
 * Intent action broadcast when the status of iab changed
 */
const val INTENT_IAB_STATUS_CHANGED = "iabStatusChanged"