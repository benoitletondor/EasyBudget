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

package com.benoitletondor.easybudgetapp.iab

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

private const val SKU_PREMIUM_LEGACY = "premium"
private const val SKU_PREMIUM_SUBSCRIPTION = "premium_subscription"
private const val SKU_PRO_SUBSCRIPTION = "pro_subscription"

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
        return iabStatusMutableFlow.value.isFinal()
    }

    private fun PremiumCheckStatus.isFinal() = when(this) {
        PremiumCheckStatus.INITIALIZING,
        PremiumCheckStatus.CHECKING -> false
        PremiumCheckStatus.ERROR,
        PremiumCheckStatus.NOT_PREMIUM,
        PremiumCheckStatus.LEGACY_PREMIUM,
        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
        PremiumCheckStatus.PRO_SUBSCRIBED -> true
    }

    /**
     * Is the user a premium user
     *
     * @return true if we could verify that the user is premium, false otherwise
     */
    override suspend fun isUserPremium(): Boolean {
        var status = iabStatusMutableFlow.first { it.isFinal() }

        // Try to avoid sending false negative, it seems that it can happen when launching the app
        if (status === PremiumCheckStatus.NOT_PREMIUM || status === PremiumCheckStatus.ERROR) {
            delay(250)
            status = iabStatusMutableFlow.first { it.isFinal() }
        }

        return when(status) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING,
            PremiumCheckStatus.ERROR,
            PremiumCheckStatus.NOT_PREMIUM -> false
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.PRO_SUBSCRIBED -> true
        }
    }

    override suspend fun isUserPro(): Boolean {
        var status = iabStatusMutableFlow.first { it.isFinal() }

        // Try to avoid sending false negative, it seems that it can happen when launching the app
        if (status === PremiumCheckStatus.NOT_PREMIUM || status === PremiumCheckStatus.ERROR) {
            delay(250)
            status = iabStatusMutableFlow.first { it.isFinal() }
        }

        return when(status) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING,
            PremiumCheckStatus.ERROR,
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> false
            PremiumCheckStatus.PRO_SUBSCRIBED -> true
        }
    }

    /**
     * Update the current IAP status if already checked
     */
    override fun updateIAPStatusIfNeeded() {
        Logger.debug("updateIAPStatusIfNeeded: ${iabStatusMutableFlow.value}")

        when(iabStatusMutableFlow.value) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> Unit
            PremiumCheckStatus.ERROR -> startBillingClient()
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.PRO_SUBSCRIBED -> {
                setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
                queryPurchases()
            }
        }
    }

    override suspend fun fetchPricingOrDefault(): Pricing {
        try {
            if(iabStatusMutableFlow.value == PremiumCheckStatus.INITIALIZING || iabStatusMutableFlow.value == PremiumCheckStatus.ERROR) {
                throw IllegalStateException("IAB is not setup")
            }

            val skuList = listOf(
                SKU_PREMIUM_SUBSCRIPTION,
                SKU_PRO_SUBSCRIPTION,
            )

            val productList = skuList.map { productId ->
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            val (billingResult, skuDetailsList) = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()
            )

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                throw IllegalStateException("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
            }

            if(skuDetailsList == null) {
                throw IllegalStateException("Unable to get details from PlayStore. Please restart the app and try again")
            }

            val premiumSubscriptionPrice = skuDetailsList
                .first { it.productId == SKU_PREMIUM_SUBSCRIPTION }
                .subscriptionOfferDetails
                ?.first()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.first()
                ?.formattedPrice
                ?: throw IllegalArgumentException("No price for premium subscription")

            val proSubscriptionPrice = skuDetailsList
                .first { it.productId == SKU_PRO_SUBSCRIPTION }
                .subscriptionOfferDetails
                ?.first()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.first()
                ?.formattedPrice
                ?: throw IllegalArgumentException("No price for pro subscription")

            return Pricing(
                premiumPricing = premiumSubscriptionPrice,
                proPricing = proSubscriptionPrice,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Logger.error("Error while fetching pricing, returning default", e)

            return Pricing(
                premiumPricing = appContext.getString(R.string.premium_subscription_price),
                proPricing = appContext.getString(R.string.pro_subscription_price),
            )
        }

    }

    private fun queryPurchases() {
        queryPurchasesJob?.cancel()
        queryPurchasesJob = scope.launch {
            val subscriptionsResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
            )

            // Is it a failure?
            if (subscriptionsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error(
                    "Error while querying iab inventory: ${subscriptionsResult.billingResult.responseCode}",
                    Exception("Unable to query subscriptions"),
                )
                setIabStatusAndNotify(PremiumCheckStatus.ERROR)
                return@launch
            }

            val premiumSubscriptions = subscriptionsResult.purchasesList.filter { it.products.contains(SKU_PREMIUM_SUBSCRIPTION) }
            val proSubscriptions = subscriptionsResult.purchasesList.filter { it.products.contains(SKU_PRO_SUBSCRIPTION) }

            Logger.debug("iab query inventory was successful: premium subscribed: ${premiumSubscriptions.isNotEmpty()}, pro subscribed: ${proSubscriptions.isNotEmpty()}")

            if (proSubscriptions.isNotEmpty()) {
                proSubscriptions
                    .filter { !it.isAcknowledged }
                    .forEach { purchase ->
                        Logger.warning("Found a not ACKed Pro purchase, taking care of it", Exception("Non ACKed Pro purchase"))

                        val ackResult = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())

                        if( ackResult.responseCode != BillingClient.BillingResponseCode.OK ) {
                            Logger.error(
                                "Error while sending ACK for old Pro purchase: ${ackResult.responseCode}: ${ackResult.debugMessage}",
                                Exception("Pro subscription late ACK error"),
                            )
                        }
                    }

                setIabStatusAndNotify(PremiumCheckStatus.PRO_SUBSCRIBED)
                return@launch
            }

            if (premiumSubscriptions.isNotEmpty()) {
                premiumSubscriptions
                    .filter { !it.isAcknowledged }
                    .forEach { purchase ->
                        Logger.warning("Found a not ACKed premium purchase, taking care of it", Exception("Non ACKed Premium purchase"))

                        val ackResult = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())

                        if( ackResult.responseCode != BillingClient.BillingResponseCode.OK ) {
                            Logger.error(
                                "Error while sending ACK for old Premium purchase: ${ackResult.responseCode}: ${ackResult.debugMessage}",
                                Exception("Premium subscription late ACK error"),
                            )
                        }
                    }

                setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                return@launch
            }


            val legacyPremiumResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            )

            // Is it a failure?
            if (legacyPremiumResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error(
                    "Error while querying iab inventory: ${legacyPremiumResult.billingResult.responseCode}: ${legacyPremiumResult.billingResult.debugMessage}",
                    Exception("Unable to query legacy purchase")
                )
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
        when(iabStatusMutableFlow.value) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> return PurchaseFlowResult.Error("Runtime error: ${iabStatusMutableFlow.value}")
            PremiumCheckStatus.ERROR -> return PurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
            PremiumCheckStatus.NOT_PREMIUM -> Unit
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.PRO_SUBSCRIBED -> return PurchaseFlowResult.Error("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(SKU_PREMIUM_SUBSCRIPTION)
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
                return PurchaseFlowResult.Success(purchaseType = PurchaseType.PREMIUM_SUBSCRIPTION)
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

        return pendingPurchaseEventMutableFlow.first {
            when(it) {
                PurchaseFlowResult.Cancelled,
                is PurchaseFlowResult.Error -> true
                is PurchaseFlowResult.Success -> it.purchaseType == PurchaseType.PREMIUM_SUBSCRIPTION
            }
        }
    }

    override suspend fun launchProSubscriptionFlow(activity: Activity): PurchaseFlowResult {
        when(iabStatusMutableFlow.value) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> return PurchaseFlowResult.Error("Runtime error: ${iabStatusMutableFlow.value}")
            PremiumCheckStatus.ERROR -> return PurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> Unit
            PremiumCheckStatus.PRO_SUBSCRIBED -> return PurchaseFlowResult.Error("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(SKU_PRO_SUBSCRIPTION)
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
                setIabStatusAndNotify(PremiumCheckStatus.PRO_SUBSCRIBED)
                return PurchaseFlowResult.Success(purchaseType = PurchaseType.PRO_SUBSCRIPTION)
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

        if (iabStatusMutableFlow.value == PremiumCheckStatus.PREMIUM_SUBSCRIBED) {
            val subscriptionsResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
            )

            if (subscriptionsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error(
                    "Error while querying iab inventory: ${subscriptionsResult.billingResult.responseCode}: ${subscriptionsResult.billingResult.debugMessage}",
                    Exception("Unable to query current subscriptions for upgrade"),
                )
                return PurchaseFlowResult.Error("Unable to fetch content from PlayStore (premium subscription fetch response code: ${subscriptionsResult.billingResult.responseCode}). Please restart the app and try again")
            }

            val premiumSubscription = subscriptionsResult.purchasesList.firstOrNull { it.products.contains(SKU_PREMIUM_SUBSCRIPTION) }
            if (premiumSubscription == null) {
                Logger.error(
                    "Unable to find premium subscription",
                    Exception("Unable to find premium subscription to upgrade")
                )
                return PurchaseFlowResult.Error("Unable to find your premium subscription. Please restart the app and try again")
            }

            val subscriptionUpdateParam = SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(premiumSubscription.purchaseToken)
                .setSubscriptionReplacementMode(SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .setSubscriptionUpdateParams(subscriptionUpdateParam)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        } else {
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }

        return pendingPurchaseEventMutableFlow.first {
            when(it) {
                PurchaseFlowResult.Cancelled,
                is PurchaseFlowResult.Error -> true
                is PurchaseFlowResult.Success -> it.purchaseType == PurchaseType.PRO_SUBSCRIPTION
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Logger.debug("iab setup finished.")

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            Logger.error(
                "Error while setting-up iab: ${billingResult.responseCode}: ${billingResult.debugMessage}",
                Exception("Unable to setup IAB"),
            )
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
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Logger.debug("Purchase cancelled")
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Cancelled)
                    }
                    else -> {
                        Logger.error(
                            "Error while purchasing: ${billingResult.responseCode}: ${billingResult.debugMessage}",
                            Exception("Error while purchasing")
                        )
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("An error occurred (status code: " + billingResult.responseCode + ")"))
                    }
                }

                return@launch
            }

            if ( purchases.isNullOrEmpty() ) {
                pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("No purchased item found"))
                return@launch
            }

            Logger.debug("Purchase successful.")

            for (purchase in purchases) {
                if (purchase.products.contains(SKU_PREMIUM_LEGACY) ||
                    purchase.products.contains(SKU_PREMIUM_SUBSCRIPTION) ||
                    purchase.products.contains(SKU_PRO_SUBSCRIPTION))
                {
                    val ackResult = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())

                    if( ackResult.responseCode != BillingClient.BillingResponseCode.OK ) {
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Error("Error when acknowledging purchase with Google (${ackResult.responseCode}, ${ackResult.debugMessage}). Please try again"))
                        return@launch
                    }

                    if (purchase.products.contains(SKU_PREMIUM_LEGACY)) {
                        setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Success(purchaseType = PurchaseType.LEGACY_PREMIUM))
                    } else if(purchase.products.contains(SKU_PREMIUM_SUBSCRIPTION)) {
                        setIabStatusAndNotify(PremiumCheckStatus.PREMIUM_SUBSCRIBED)
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Success(purchaseType = PurchaseType.PREMIUM_SUBSCRIPTION))
                    } else if(purchase.products.contains(SKU_PRO_SUBSCRIPTION)) {
                        setIabStatusAndNotify(PremiumCheckStatus.PRO_SUBSCRIBED)
                        pendingPurchaseEventMutableFlow.emit(PurchaseFlowResult.Success(purchaseType = PurchaseType.PRO_SUBSCRIPTION))
                    }

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