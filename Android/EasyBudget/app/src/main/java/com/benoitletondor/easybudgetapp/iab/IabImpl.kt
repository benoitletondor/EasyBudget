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
import com.benoitletondor.easybudgetapp.parameters.Parameters
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * SKU premium
 */
private const val SKU_PREMIUM = "premium"
/**
 * Is the user premium from AppTurbo (bool)
 */
private const val APP_TURBO_PREMIUM_PARAMETER_KEY = "appturbo_offer"
/**
 * Cache storage of the IAB status
 */
private const val PREMIUM_PARAMETER_KEY = "premium"
/**
 * Has the user redeemed a Batch offer (bool)
 */
private const val BATCH_OFFER_REDEEMED_PARAMETER_KEY = "batch_offer_redeemed"

class IabImpl(
    context: Context,
    private val parameters: Parameters,
) : Iab, PurchasesUpdatedListener, BillingClientStateListener, PurchasesResponseListener, AcknowledgePurchaseResponseListener {

    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * iab check status
     */
    private var iabStatus: PremiumCheckStatus = PremiumCheckStatus.INITIALIZING

    private var premiumFlowContinuation: Continuation<PremiumPurchaseFlowResult>? = null

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
        iabStatus = status

        // Save status only on success
        if (status == PremiumCheckStatus.PREMIUM || status == PremiumCheckStatus.NOT_PREMIUM) {
            parameters.setUserPremium(iabStatus == PremiumCheckStatus.PREMIUM)
        }

        val intent = Intent(INTENT_IAB_STATUS_CHANGED)

        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
    }

    /**
     * Is the user a premium user
     *
     * @return true if the user if premium, false otherwise
     */
    override fun isUserPremium(): Boolean
    {
        return parameters.isUserPremium() ||
            parameters.getBoolean(BATCH_OFFER_REDEEMED_PARAMETER_KEY, false) ||
            parameters.getBoolean(APP_TURBO_PREMIUM_PARAMETER_KEY, false) ||
            iabStatus == PremiumCheckStatus.PREMIUM
    }

    /**
     * Update the current IAP status if already checked
     */
    override fun updateIAPStatusIfNeeded() {
        Logger.debug("updateIAPStatusIfNeeded: $iabStatus")

        if ( iabStatus == PremiumCheckStatus.NOT_PREMIUM ) {
            setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                this
            )
        } else if ( iabStatus == PremiumCheckStatus.ERROR ) {
            startBillingClient()
        }
    }

    /**
     * Launch the premium purchase flow
     *
     * @param activity activity that started this purchase
     */
    override suspend fun launchPremiumPurchaseFlow(activity: Activity): PremiumPurchaseFlowResult {
        if ( iabStatus != PremiumCheckStatus.NOT_PREMIUM ) {
            return when (iabStatus) {
                PremiumCheckStatus.ERROR -> PremiumPurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.PREMIUM -> PremiumPurchaseFlowResult.Error("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
                else -> PremiumPurchaseFlowResult.Error("Runtime error: $iabStatus")
            }
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(SKU_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val (billingResult, skuDetailsList) = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(skuList)
                .build()
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
                return PremiumPurchaseFlowResult.Success
            }

            return PremiumPurchaseFlowResult.Error("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if (skuDetailsList == null || skuDetailsList.isEmpty()) {
            return PremiumPurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
        }

        return suspendCoroutine { continuation ->
            premiumFlowContinuation = continuation

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(skuDetailsList[0])
                        .build()
                )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Logger.debug("iab setup finished.")

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Oh noes, there was a problem.
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            Logger.error("Error while setting-up iab: " + billingResult.responseCode)
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.CHECKING)

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            this
        )
    }

    override fun onBillingServiceDisconnected() {
        Logger.debug("onBillingServiceDisconnected")

        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("Lost connection with Google Play")))
        setIabStatusAndNotify(PremiumCheckStatus.ERROR)
    }

    override fun onQueryPurchasesResponse(billingResult: BillingResult, purchaseHistoryRecordList: MutableList<Purchase>) {
        Logger.debug("iab query inventory finished.")

        // Is it a failure?
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Logger.error("Error while querying iab inventory: " + billingResult.responseCode)
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            return
        }

        var premium = false
        for (purchase in purchaseHistoryRecordList) {
            if (purchase.products.contains(SKU_PREMIUM)) {
                premium = true
            }
        }

        Logger.debug("iab query inventory was successful: $premium")

        setIabStatusAndNotify(if (premium) PremiumCheckStatus.PREMIUM else PremiumCheckStatus.NOT_PREMIUM)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Logger.debug("Purchase finished: " + billingResult.responseCode)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Logger.error("Error while purchasing premium: " + billingResult.responseCode)
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Cancelled))
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
                    premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Success))
                    return
                }
                else -> premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("An error occurred (status code: " + billingResult.responseCode + ")")))
            }

            premiumFlowContinuation = null
            return
        }


        if ( purchases.isNullOrEmpty() ) {
            premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("No purchased item found")))
            premiumFlowContinuation = null
            return
        }

        Logger.debug("Purchase successful.")

        for (purchase in purchases) {
            if (purchase.products.contains(SKU_PREMIUM)) {
                billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(), this)
                return
            }
        }

        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("No purchased item found")))
        premiumFlowContinuation = null
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        Logger.debug("Acknowledge successful.")

        if( billingResult.responseCode != BillingClient.BillingResponseCode.OK ) {
            premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("Error when acknowledging purchase with Google (${billingResult.responseCode}, ${billingResult.debugMessage}). Please try again")))
            premiumFlowContinuation = null
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Success))
        premiumFlowContinuation = null
    }
}

private fun Parameters.setUserPremium(premium: Boolean) {
    putBoolean(PREMIUM_PARAMETER_KEY, premium)
}

private fun Parameters.isUserPremium(): Boolean {
    return getBoolean(PREMIUM_PARAMETER_KEY, false)
}

private enum class PremiumCheckStatus {
    INITIALIZING,

    CHECKING,

    ERROR,

    NOT_PREMIUM,

    PREMIUM
}