package com.benoitletondor.easybudgetapp.iab

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.*
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.Parameters
import java.lang.ref.WeakReference
import java.util.*

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

class IabImpl(context: Context,
              private val parameters: Parameters) : Iab, PurchasesUpdatedListener, BillingClientStateListener, PurchaseHistoryResponseListener, SkuDetailsResponseListener {
    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * iab check status
     */
    private var iabStatus: PremiumCheckStatus = PremiumCheckStatus.INITIALIZING
    /**
     * Listener for the current purchase
     */
    private var premiumPurchaseListener: PremiumPurchaseListener? = null
    /**
     * Activity that triggered the current purchase
     */
    private var purchaseActivity = WeakReference<Activity>(null)

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
            parameters.putBoolean(PREMIUM_PARAMETER_KEY, iabStatus == PremiumCheckStatus.PREMIUM)
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
        return parameters.getBoolean(PREMIUM_PARAMETER_KEY, false) ||
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
            billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this)
        } else if ( iabStatus == PremiumCheckStatus.ERROR ) {
            startBillingClient()
        }
    }

    /**
     * Launch the premium purchase flow
     *
     * @param activity activity that started this purchase
     * @param listener listener for purchase events
     */
    override fun launchPremiumPurchaseFlow(activity: Activity, listener: PremiumPurchaseListener) {
        if (iabStatus != PremiumCheckStatus.NOT_PREMIUM) {
            when (iabStatus) {
                PremiumCheckStatus.ERROR -> listener.onPurchaseError("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.PREMIUM -> listener.onPurchaseError("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
                else -> listener.onPurchaseError("Runtime error: $iabStatus")
            }

            return
        }

        premiumPurchaseListener = listener
        purchaseActivity = WeakReference(activity)

        val skuList = ArrayList<String>(1)
        skuList.add(SKU_PREMIUM)

        Logger.debug("Launching querySkuDetailsAsync")

        billingClient.querySkuDetailsAsync(
            SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build(),
            this
        )
    }

    override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: List<SkuDetails>) {
        Logger.debug("onSkuDetailsResponse")
        val activity = purchaseActivity.get()
        val listener = premiumPurchaseListener

        if (activity == null || listener == null) {
            Logger.debug("onSkuDetailsResponse: activity or listener null")
            return
        }

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
                listener.onPurchaseSuccess()
                return
            }

            Objects.requireNonNull<PremiumPurchaseListener>(premiumPurchaseListener).onPurchaseError("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
            return
        }

        if (skuDetailsList.isEmpty()) {
            Objects.requireNonNull<PremiumPurchaseListener>(premiumPurchaseListener).onPurchaseError("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
            return
        }

        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetailsList[0])
            .build()
        )
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

        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this)
    }

    override fun onBillingServiceDisconnected() {
        Logger.debug("onBillingServiceDisconnected")

        setIabStatusAndNotify(PremiumCheckStatus.ERROR)
    }

    override fun onPurchaseHistoryResponse(billingResult: BillingResult, purchaseHistoryRecordList: List<PurchaseHistoryRecord>?) {
        Logger.debug("iab query inventory finished.")

        // Is it a failure?
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Logger.error("Error while querying iab inventory: " + billingResult.responseCode)
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            return
        }

        var premium = false
        if (purchaseHistoryRecordList != null) {
            for (purchase in purchaseHistoryRecordList) {
                if (SKU_PREMIUM == purchase.sku) {
                    premium = true
                }
            }
        }

        Logger.debug("iab query inventory was successful: $premium")

        setIabStatusAndNotify(if (premium) PremiumCheckStatus.PREMIUM else PremiumCheckStatus.NOT_PREMIUM)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val listener = premiumPurchaseListener
        premiumPurchaseListener = null

        if (listener == null) {
            return
        }

        Logger.debug("Purchase finished: " + billingResult.responseCode)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Logger.error("Error while purchasing premium: " + billingResult.responseCode)
            when {
                billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> listener.onUserCancelled()
                billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
                    listener.onPurchaseSuccess()
                    return
                }
                else -> listener.onPurchaseError("An error occurred (status code: " + billingResult.responseCode + ")")
            }

            return
        }

        Logger.debug("Purchase successful.")

        if ( purchases.isNullOrEmpty() ) {
            listener.onPurchaseError("No purchased item found")
            return
        }

        var premiumBought = false
        for (purchase in purchases) {
            if (SKU_PREMIUM == purchase.sku) {
                premiumBought = true
            }
        }

        if (!premiumBought) {
            listener.onPurchaseError("No purchased item found")
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.PREMIUM)
        listener.onPurchaseSuccess()
    }
}

private enum class PremiumCheckStatus {
    INITIALIZING,

    CHECKING,

    ERROR,

    NOT_PREMIUM,

    PREMIUM
}