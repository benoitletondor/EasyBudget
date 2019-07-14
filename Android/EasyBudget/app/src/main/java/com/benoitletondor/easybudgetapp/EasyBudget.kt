/*
 *   Copyright 2019 Benoit LETONDOR
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

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.batch.android.Batch
import com.batch.android.BatchActivityLifecycleHelper
import com.batch.android.BatchNotificationChannelsManager.DEFAULT_CHANNEL_ID
import com.batch.android.Config
import com.batch.android.PushNotificationType
import com.benoitletondor.easybudgetapp.BuildVersion.VERSION_2_0_10
import com.benoitletondor.easybudgetapp.BuildVersion.VERSION_2_0_13
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.injection.appModule
import com.benoitletondor.easybudgetapp.injection.viewModelModule
import com.benoitletondor.easybudgetapp.notif.DarkThemeNotif
import com.benoitletondor.easybudgetapp.notif.CHANNEL_DAILY_REMINDERS
import com.benoitletondor.easybudgetapp.notif.CHANNEL_MONTHLY_REMINDERS
import com.benoitletondor.easybudgetapp.notif.CHANNEL_NEW_FEATURES
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.push.PushService.Companion.DAILY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.push.PushService.Companion.MONTHLY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.settings.SettingsActivity
import com.benoitletondor.easybudgetapp.view.getRatingPopupUserStep
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.*

/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
class EasyBudget : Application() {
    private val iab: Iab by inject()
    private val parameters: Parameters by inject()

    // This is injected only to ensure it's created, it is closed right after onCreate
    private val db: DB by inject()

// ------------------------------------------>

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@EasyBudget)
            modules(listOf(appModule, viewModelModule))
        }

        // Init actions
        init()

        // Check if an update occurred and perform action if needed
        checkUpdateAction()

        // Crashlytics
        if ( BuildConfig.CRASHLYTICS_ACTIVATED ) {
            Fabric.with(this, Crashlytics())

            Crashlytics.setUserIdentifier(parameters.getLocalId())
        }

        // Ensure DB is created
        db.use {
            it.ensureDBCreated()
        }

        // Batch
        setUpBatchSDK()

        // Setup theme
        AppCompatDelegate.setDefaultNightMode(parameters.getTheme().toPlatformValue())
    }

    /**
     * Init app const and parameters
     */
    private fun init() {
        /*
         * Save first launch date if needed
         */
        val initDate = parameters.getInitTimestamp()
        if (initDate <= 0) {
            Logger.debug("Registering first launch date")

            parameters.setInitTimestamp(Date().time)
            parameters.setUserCurrency(Currency.getInstance(Locale.getDefault())) // Set a default currency before onboarding
        }

        /*
         * Create local ID if needed
         */
        var localId = parameters.getLocalId()
        if (localId == null) {
            localId = UUID.randomUUID().toString()
            Logger.debug("Generating local id : $localId")

            parameters.setLocalId(localId)
        } else {
            Logger.debug("Local id : $localId")
        }

        // Activity counter for app foreground & background
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activityCounter = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (activityCounter == 0) {
                    onAppForeground(activity)
                }

                activityCounter++
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (activityCounter == 1) {
                    onAppBackground()
                }

                activityCounter--
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Show the rating popup if the user didn't asked not to every day after the app has been open
     * in 3 different days.
     */
    private fun showRatingPopupIfNeeded(activity: Activity) {
        try {
            if (activity !is MainActivity) {
                Logger.debug("Not showing rating popup cause app is not opened by the MainActivity")
                return
            }

            val dailyOpens = parameters.getNumberOfDailyOpen()
            if (dailyOpens > 2) {
                if (!hasRatingPopupBeenShownToday()) {
                    val shown = RatingPopup(activity, parameters).show(false)
                    if (shown) {
                        parameters.setRatingPopupLastAutoShowTimestamp(Date().time)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("Error while showing rating popup", e)
        }

    }

    private fun showPremiumPopupIfNeeded(activity: Activity) {
        try {
            if (activity !is MainActivity) {
                return
            }

            if ( parameters.hasPremiumPopupBeenShow() ) {
                return
            }

            if ( iab.isUserPremium() ) {
                return
            }

            if ( !parameters.hasUserCompleteRating() ) {
                return
            }

            val currentStep = parameters.getRatingPopupUserStep()
            if (currentStep == RatingPopup.RatingPopupStep.STEP_LIKE ||
                currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_NOT_RATED ||
                currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_RATED) {
                if ( !hasRatingPopupBeenShownToday() && shouldShowPremiumPopup() ) {
                    parameters.setPremiumPopupLastAutoShowTimestamp(Date().time)

                    AlertDialog.Builder(activity)
                        .setTitle(R.string.premium_popup_become_title)
                        .setMessage(R.string.premium_popup_become_message)
                        .setPositiveButton(R.string.premium_popup_become_cta) { dialog13, _ ->
                            val startIntent = Intent(activity, SettingsActivity::class.java)
                            startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true)
                            ActivityCompat.startActivity(activity, startIntent, null)

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
        } catch (e: Exception) {
            Logger.error("Error while showing become premium popup", e)
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

    /**
     * Set-up Batch SDK config + lifecycle
     */
    private fun setUpBatchSDK() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Monthly report channel
            val name = getString(R.string.setting_category_notifications_monthly_title)
            val description = getString(R.string.setting_category_notifications_monthly_message)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val monthlyReportChannel = NotificationChannel(CHANNEL_MONTHLY_REMINDERS, name, importance)
            monthlyReportChannel.description = description

            // Daily reminder channel
            val dailyName = getString(R.string.setting_category_notifications_daily_title)
            val dailyDescription = getString(R.string.setting_category_notifications_daily_message)
            val dailyImportance = NotificationManager.IMPORTANCE_DEFAULT

            val dailyReportChannel = NotificationChannel(CHANNEL_DAILY_REMINDERS, dailyName, dailyImportance)
            dailyReportChannel.description = dailyDescription

            // New features channel
            val newFeatureName = getString(R.string.setting_category_notifications_update_title)
            val newFeatureDescription = getString(R.string.setting_category_notifications_update_message)
            val newFeatureImportance = NotificationManager.IMPORTANCE_LOW

            val newFeatureChannel = NotificationChannel(CHANNEL_NEW_FEATURES, newFeatureName, newFeatureImportance)
            newFeatureChannel.description = newFeatureDescription

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(newFeatureChannel)
                notificationManager.createNotificationChannel(monthlyReportChannel)
                notificationManager.createNotificationChannel(dailyReportChannel)

                // Remove Batch's default
                notificationManager.deleteNotificationChannel(DEFAULT_CHANNEL_ID)
            }
        }

        Batch.setConfig(Config(BuildConfig.BATCH_API_KEY).setCanUseAdvertisingID(false))
        Batch.Push.setManualDisplay(true)
        Batch.Push.setSmallIconResourceId(R.drawable.ic_push)
        Batch.Push.setNotificationsColor(ContextCompat.getColor(this, R.color.accent))
        Batch.Push.getChannelsManager().setChannelIdInterceptor { payload, _ ->
            if ( "true".equals(payload.pushBundle.getString(DAILY_REMINDER_KEY), ignoreCase = true) ) {
                return@setChannelIdInterceptor CHANNEL_DAILY_REMINDERS
            }

            if ( "true".equals(payload.pushBundle.getString(MONTHLY_REMINDER_KEY), ignoreCase = true) ) {
                return@setChannelIdInterceptor CHANNEL_MONTHLY_REMINDERS
            }

            CHANNEL_NEW_FEATURES
        }

        // Remove vibration & sound
        val notificationTypes = EnumSet.allOf(PushNotificationType::class.java)
        notificationTypes.remove(PushNotificationType.VIBRATE)
        notificationTypes.remove(PushNotificationType.SOUND)
        Batch.Push.setNotificationsType(notificationTypes)

        registerActivityLifecycleCallbacks(BatchActivityLifecycleHelper())
    }

    /**
     * Check if a an update occured and call [.onUpdate] if so
     */
    private fun checkUpdateAction() {
        val savedVersion = parameters.getCurrentAppVersion()
        if (savedVersion > 0 && savedVersion != BuildConfig.VERSION_CODE) {
            onUpdate(savedVersion, BuildConfig.VERSION_CODE)
        }

        parameters.setCurrentAppVersion(BuildConfig.VERSION_CODE)
    }

    /**
     * Called when an update occurred
     */
    private fun onUpdate(previousVersion: Int, @Suppress("SameParameterValue") newVersion: Int) {
        Logger.debug("Update detected, from $previousVersion to $newVersion")

        if( previousVersion < VERSION_2_0_10 && newVersion <= VERSION_2_0_13 && iab.isUserPremium() ) {
            DarkThemeNotif.showDarkThemeNotif(this)
        }
    }

// -------------------------------------->

    /**
     * Called when the app goes foreground
     *
     * @param activity The activity that gone foreground
     */
    private fun onAppForeground(activity: Activity) {
        Logger.debug("onAppForeground")

        /*
         * Increment the number of open
         */
        parameters.setNumberOfOpen(parameters.getNumberOfOpen() + 1)

        /*
         * Check if last open is from another day
         */
        var shouldIncrementDailyOpen = false

        val lastOpen = parameters.getLastOpenTimestamp()
        if (lastOpen > 0) {
            val cal = Calendar.getInstance()
            cal.time = Date(lastOpen)

            val lastDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = Date()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            if (lastDay != currentDay) {
                shouldIncrementDailyOpen = true
            }
        } else {
            shouldIncrementDailyOpen = true
        }

        // Increment daily open
        if (shouldIncrementDailyOpen) {
            parameters.setNumberOfDailyOpen(parameters.getNumberOfDailyOpen() + 1)
        }

        /*
         * Save last open date
         */
        parameters.setLastOpenTimestamp(Date().time)

        /*
         * Rating popup every day after 3 opens
         */
        showRatingPopupIfNeeded(activity)

        /*
         * Premium popup after rating complete
         */
        showPremiumPopupIfNeeded(activity)

        /*
         * Update iap status if needed
         */
        iab.updateIAPStatusIfNeeded()
    }

    /**
     * Called when the app goes background
     */
    private fun onAppBackground() {
        Logger.debug("onAppBackground")
    }
}
