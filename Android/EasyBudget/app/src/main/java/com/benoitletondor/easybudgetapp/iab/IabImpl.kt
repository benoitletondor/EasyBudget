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
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.*
import com.benoitletondor.easybudgetapp.helper.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * SKU premium
 */
private const val SKU_PREMIUM_LEGACY = "premium"
/**
 * SKU premium
 */
private const val SKU_SUBSCRIPTION = "premium_subscription"

class IabImpl(
    context: Context,
) : Iab, PurchasesUpdatedListener, BillingClientStateListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingPurchaseEventMutableFlow = MutableSharedFlow<PurchaseFlowResult>()

    private var queryPurchasesJob: Job? = null

    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * iab check status
     */
    private val iabStatusMutableFlow = MutableStateFlow(PremiumCheckStatus.INITIALIZING)
    override val iabStatusFlow: StateFlow<PremiumCheckStatus> = iabStatusMutableFlow

    init {
        startBillingClient()
    }

    private fun startBillingClient() {
        try {
            setIabStatusAndNotify(PremiumCheckStatus.INITIALIZING)

            billingClient.startConnection(this)
        } catch (e: Exception) {
            Logger.error("Error while checking iab status", e)
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
        }
    }

    /**
     * Set the new iab status and notify the app by sending an [.INTENT_IAB_STATUS_CHANGED] intent
     *
     * @param status the new status
     */
    private fun setIabStatusAndNotify(status: PremiumCheckStatus) {
        iabStatusMutableFlow.value = status

        val intent = Intent(INTENT_IAB_STATUS_CHANGED)

        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
    }

    override fun isIabReady(): Boolean {
        return isUserPremium() || iabStatusMutableFlow.value == PremiumCheckStatus.NOT_PREMIUM
    }

    /**
     * Is the user a premium user
     *
     * @return true if the user if premium, false otherwise
     */
    override fun isUserPremium(): Boolean
    {
        return (iabStatusMutableFlow.value == PremiumCheckStatus.LEGACY_PREMIUM ||
            iabStatusMutableFlow.value == PremiumCheckStatus.PREMIUM_SUBSCRIBED ||
            iabStatusMutableFlow.value == PremiumCheckStatus.PRO_SUBSCRIBED)
    }

    override fun isUserPro(): Boolean {
        return iabStatusMutableFlow.value == PremiumCheckStatus.PRO_SUBSCRIBED
    }

    override suspend fun waitForIsUserPremiumResponse(): Boolean {
        if (isUserPremium()) {
            return true
        }

        val status = iabStatusMutableFlow.first {
            it == PremiumCheckStatus.LEGACY_PREMIUM ||
                it == PremiumCheckStatus.PREMIUM_SUBSCRIBED ||
                it == PremiumCheckStatus.ERROR ||
                it == PremiumCheckStatus.NOT_PREMIUM
        }

        return when(status) {
            PremiumCheckStatus.INITIALIZING -> false
            PremiumCheckStatus.CHECKING -> false
            PremiumCheckStatus.ERROR -> false
            PremiumCheckStatus.NOT_PREMIUM -> false
            PremiumCheckStatus.LEGACY_PREMIUM -> true
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> true
            PremiumCheckStatus.PRO_SUBSCRIBED -> true
        }
    }

    /**
     * Update the current IAP status if already checked
     */
    override fun updateIAPStatusIfNeeded() {
        Logger.debug("updateIAPStatusIfNeeded: ${iabStatusMutableFlow.value}")

        if ( iabStatusMutableFlow.value == PremiumCheckStatus.NOT_PREMIUM ) {
            setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
            queryPurchases()
        } else if ( iabStatusMutableFlow.value == PremiumCheckStatus.ERROR ) {
            startBillingClient()
        }
    }

    private fun queryPurchases() {
        queryPurchasesJob?.cancel()
        queryPurchasesJob = scope.launch {
            val subscribedToPremiumResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
            )

            // Is it a failure?
            if (subscribedToPremiumResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while querying iab inventory: " + subscribedToPremiumResult.billingResult.responseCode)
                setIabStatusAndNotify(PremiumCheckStatus.ERROR)
                return@launch
            }

            val subscribed = subscribedToPremiumResult.purchasesList.any { it.products.contains(SKU_SUBSCRIPTION) }

            Logger.debug("iab query inventory was successful: $subscribed")

            if (subscribed) {
                setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                return@launch
            }

            val legacyPremiumResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            )

            // Is it a failure?
            if (legacyPremiumResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while querying iab inventory: " + legacyPremiumResult.billingResult.responseCode)
                setIabStatusAndNotify(PremiumCheckStatus.ERROR)
                return@launch
            }

            val legacyPremium = legacyPremiumResult.purchasesList.any { it.products.contains(SKU_PREMIUM_LEGACY) }

            Logger.debug("legacy iab query inventory was successful: $legacyPremium")

            if (legacyPremium) {
                setIabStatusAndNotify(PremiumCheckStatus.LEGACY_PREMIUM)
            } else {
                setIabStatusAndNotify(PremiumCheckStatus.NOT_PREMIUM)
            }
        }
    }

    override suspend fun launchPremiumSubscriptionFlow(activity: Activity): PurchaseFlowResult {
        if ( iabStatusMutableFlow.value != PremiumCheckStatus.NOT_PREMIUM ) {
            return when (iabStatusMutableFlow.value) {
                PremiumCheckStatus.ERROR -> PurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.LEGACY_PREMIUM, PremiumCheckStatus.PREMIUM_SUBSCRIBED -> PurchaseFlowResult.Error("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
                else -> PurchaseFlowResult.Error("Runtime error: ${iabStatusMutableFlow.value}")
            }
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(SKU_SUBSCRIPTION)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val (billingResult, skuDetailsList) = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(skuList)
                .build()
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                return PurchaseFlowResult.Success
            }

            return PurchaseFlowResult.Error("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if (skuDetailsList.isNullOrEmpty()) {
            return PurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
        }

        val product = skuDetailsList.first()
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return PurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: null offerToken). Please restart the app and try again")

        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .setOfferToken(offerToken)
                    .build()
            )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)

        return pendingPurchaseEventMutableFlow.first()
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Logger.debug("iab setup finished.")

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            Logger.error("Error while setting-up iab: " + billingResult.responseCode)
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
        queryPurchases()
    }

    override fun onBillingServiceDisconnected() {
        Logger.debug("onBillingServiceDisconnected")

        scope.launch {
            pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("Lost connection with Google Play"))
        }

        setIabStatusAndNotify(PremiumCheckStatus.ERROR)
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Logger.debug("Purchase finished: " + billingResult.responseCode)

        scope.launch {
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while purchasing premium: " + billingResult.responseCode)
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Cancelled)
                    else -> pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("An error occurred (status code: " + billingResult.responseCode + ")"))
                }

                return@launch
            }


            if ( purchases.isNullOrEmpty() ) {
                pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("No purchased item found"))
                return@launch
            }

            Logger.debug("Purchase successful.")

            for (purchase in purchases) {
                if (purchase.products.contains(SKU_PREMIUM_LEGACY) || purchase.products.contains(SKU_SUBSCRIPTION)) {
                    val ackResult = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())

                    if( ackResult.responseCode != BillingClient.BillingResponseCode.OK ) {
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("Error when acknowledging purchase with Google (${ackResult.responseCode}, ${ackResult.debugMessage}). Please try again"))
                        return@launch
                    }

                    setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                    pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Success)

                    return@launch
                }
            }

            pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("No purchased item found"))
        }
    }
}

enum class PremiumCheckStatus {
    INITIALIZING,

    CHECKING,

    ERROR,

    NOT_PREMIUM,

    LEGACY_PREMIUM,

    PREMIUM_SUBSCRIBED,

    PRO_SUBSCRIBED,
}