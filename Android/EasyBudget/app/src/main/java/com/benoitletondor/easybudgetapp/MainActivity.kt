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

package com.benoitletondor.easybudgetapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.compose.AppNavHost
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.centerButtons
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getNumberOfDailyOpen
import com.benoitletondor.easybudgetapp.parameters.getPremiumPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.getRatingPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.hasPremiumPopupBeenShow
import com.benoitletondor.easybudgetapp.parameters.hasUserCompleteRating
import com.benoitletondor.easybudgetapp.parameters.setPremiumPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.parameters.setPremiumPopupShown
import com.benoitletondor.easybudgetapp.parameters.setRatingPopupLastAutoShowTimestamp
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.getRatingPopupUserStep
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Main activity of the app
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var parameters: Parameters
    @Inject lateinit var iab: Iab
    @Inject lateinit var appUpdateManager: AppUpdateManager

    private val openSubscriptionScreenLiveFlow = MutableLiveFlow<Unit>()
    private val openAddExpenseScreenLiveFlow = MutableLiveFlow<Unit>()
    private val openAddRecurringExpenseScreenLiveFlow = MutableLiveFlow<Unit>()
    private val openMonthlyReportScreenLiveFlow = MutableLiveFlow<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        appUpdateManager.plugToActivityLifecycle(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.TRANSPARENT,
            )
        )

        setContent {
            AppTheme {
                AppNavHost(
                    closeApp = {
                        finish()
                    },
                    openSubscriptionScreenFlow = openSubscriptionScreenLiveFlow,
                    openAddExpenseScreenLiveFlow = openAddExpenseScreenLiveFlow,
                    openAddRecurringExpenseScreenLiveFlow = openAddRecurringExpenseScreenLiveFlow,
                    openMonthlyReportScreenFlow = openMonthlyReportScreenLiveFlow,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        showPremiumPopupIfNeeded()
        showRatingPopupIfNeeded()

        performIntentActionIfAny()
    }

    override fun onDestroy() {
        appUpdateManager.unplugFromActivityLifecycle(this)

        super.onDestroy()
    }

    private fun showPremiumPopupIfNeeded() {
        lifecycleScope.launch {
            try {
                if ( parameters.hasPremiumPopupBeenShow() ) {
                    return@launch
                }

                if ( iab.isUserPremium() || iab.iabStatusFlow.value == PremiumCheckStatus.ERROR ) {
                    return@launch
                }

                if ( !parameters.hasUserCompleteRating() ) {
                    return@launch
                }

                val currentStep = parameters.getRatingPopupUserStep()
                if (currentStep == RatingPopup.RatingPopupStep.STEP_LIKE ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_NOT_RATED ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_RATED) {
                    if ( !hasRatingPopupBeenShownToday() && shouldShowPremiumPopup() ) {
                        parameters.setPremiumPopupLastAutoShowTimestamp(Date().time)

                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(R.string.premium_popup_become_title)
                                .setMessage(R.string.premium_popup_become_message)
                                .setPositiveButton(R.string.premium_popup_become_cta) { dialog13, _ ->
                                    lifecycleScope.launch {
                                        openSubscriptionScreenLiveFlow.emit(Unit)
                                    }

                                    dialog13.dismiss()
                                }
                                .setNegativeButton(R.string.premium_popup_become_not_now) { dialog12, _ -> dialog12.dismiss() }
                                .setNeutralButton(R.string.premium_popup_become_not_ask_again) { dialog1, _ ->
                                    parameters.setPremiumPopupShown()
                                    dialog1.dismiss()
                                }
                                .show()
                                .centerButtons()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error("Error while showing become premium popup", e)
            }
        }
    }

    /**
     * Show the rating popup if the user didn't asked not to every day after the app has been open
     * in 3 different days.
     */
    private fun showRatingPopupIfNeeded() {
        try {
            val dailyOpens = parameters.getNumberOfDailyOpen()
            if (dailyOpens > 2) {
                if (!hasRatingPopupBeenShownToday()) {
                    val shown = RatingPopup(this, parameters).show(false)
                    if (shown) {
                        parameters.setRatingPopupLastAutoShowTimestamp(Date().time)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("Error while showing rating popup", e)
        }

    }

    /**
     * Has the rating popup been shown automatically today
     *
     * @return true if the rating popup has been shown today, false otherwise
     */
    private fun hasRatingPopupBeenShownToday(): Boolean {
        val lastRatingTS = parameters.getRatingPopupLastAutoShowTimestamp()
        if (lastRatingTS > 0) {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = Date(lastRatingTS)
            val lastTimeDay = cal.get(Calendar.DAY_OF_YEAR)

            return currentDay == lastTimeDay
        }

        return false
    }

    /**
     * Check that last time the premium popup was shown was 2 days ago or more
     *
     * @return true if we can show premium popup, false otherwise
     */
    private fun shouldShowPremiumPopup(): Boolean {
        val lastPremiumTS = parameters.getPremiumPopupLastAutoShowTimestamp()
        if (lastPremiumTS == 0L) {
            return true
        }

        // Set calendar to last time 00:00 + 2 days
        val cal = Calendar.getInstance()
        cal.time = Date(lastPremiumTS)
        cal.set(Calendar.HOUR, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 2)

        return Date().after(cal.time)
    }

    private fun performIntentActionIfAny(): Boolean {
        if (intent != null) {
            return try {
                openMonthlyReportIfNeeded(intent) || openAddExpenseIfNeeded(intent) || openAddRecurringExpenseIfNeeded(intent)
            } finally {
                intent = null
            }
        }

        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent
        performIntentActionIfAny()
    }

// ------------------------------------------>

    /**
     * Open the monthly report activity if the given intent contains the monthly uri part.
     *
     * @param intent
     */
    private fun openMonthlyReportIfNeeded(intent: Intent): Boolean {
        try {
            val data = intent.data
            if (data != null && "true" == data.getQueryParameter("monthly")) {
                lifecycleScope.launch {
                    openMonthlyReportScreenLiveFlow.emit(Unit)
                }

                return true
            }
        } catch (e: Exception) {
            Logger.error("Error while opening report activity", e)
        }

        return false
    }


    /**
     * Open the add expense screen if the given intent contains the [.INTENT_SHOW_ADD_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddExpenseIfNeeded(intent: Intent): Boolean {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_EXPENSE, false)) {
            lifecycleScope.launch {
                openAddExpenseScreenLiveFlow.emit(Unit)
            }

            return true
        }

        return false
    }

    /**
     * Open the add recurring expense screen if the given intent contains the [.INTENT_SHOW_ADD_RECURRING_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddRecurringExpenseIfNeeded(intent: Intent): Boolean {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_RECURRING_EXPENSE, false)) {
            lifecycleScope.launch {
                openAddRecurringExpenseScreenLiveFlow.emit(Unit)
            }

            return true
        }

        return false
    }

    companion object {
        // Those 2 are used by the shortcuts
        private const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        private const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"
    }
}