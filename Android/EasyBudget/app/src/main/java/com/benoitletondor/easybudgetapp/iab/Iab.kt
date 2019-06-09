package com.benoitletondor.easybudgetapp.iab

import android.app.Activity

interface Iab {
    fun isUserPremium(): Boolean
    fun updateIAPStatusIfNeeded()
    fun launchPremiumPurchaseFlow(activity: Activity, listener: PremiumPurchaseListener)
}

/**
 * Listener for in-app purchase buying flow
 *
 * @author Benoit LETONDOR
 */
interface PremiumPurchaseListener {
    /**
     * Called when the user cancel the purchase
     */
    fun onUserCancelled()

    /**
     * Called when an error occurred during the iab flow
     *
     * @param error the error
     */
    fun onPurchaseError(error: String)

    /**
     * Called on success
     */
    fun onPurchaseSuccess()
}

/**
 * Intent action broadcast when the status of iab changed
 */
const val INTENT_IAB_STATUS_CHANGED = "iabStatusChanged"