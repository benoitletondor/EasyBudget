/*
 *   Copyright 2015 Benoit LETONDOR
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.batch.android.Batch
import com.batch.android.BatchNotificationChannelsManager.DEFAULT_CHANNEL_ID
import com.batch.android.Config
import com.batch.android.PushNotificationType
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.injection.appModule
import com.benoitletondor.easybudgetapp.notif.DailyNotifOptinService
import com.benoitletondor.easybudgetapp.notif.MonthlyReportNotifService
import com.benoitletondor.easybudgetapp.notif.NotificationsChannels.*
import com.benoitletondor.easybudgetapp.push.PushService.DAILY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.push.PushService.MONTHLY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.view.MainActivity
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.SettingsActivity
import com.crashlytics.android.Crashlytics
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.net.URLEncoder
import java.util.*

const val DEFAULT_LOW_MONEY_WARNING_AMOUNT = 100

/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
class EasyBudget : Application() {

    /**
     * GA tracker
     */
    private lateinit var analyticsTracker: Tracker

    /**
     * In-app billing, setup
     */
    private val iab: Iab by inject()

// ------------------------------------------>

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@EasyBudget)
            modules(appModule)
        }

        // Init actions
        init()

        // Check if an update occurred and perform action if needed
        checkUpdateAction()

        // Crashlytics
        if (BuildConfig.CRASHLYTICS_ACTIVATED) {
            Fabric.with(this, Crashlytics())

            Crashlytics.setUserIdentifier(Parameters.getInstance(applicationContext).getString(ParameterKeys.LOCAL_ID))
        }

        // Batch
        setUpBatchSDK()

        // Analytics
        val analytics = GoogleAnalytics.getInstance(this)
        analytics.setDryRun(!BuildConfig.ANALYTICS_ACTIVATED)

        analyticsTracker = analytics.newTracker(R.xml.analytics)
        analyticsTracker.enableAdvertisingIdCollection(false)
    }

    /**
     * Init app const and parameters
     */
    private fun init() {
        /*
         * Save first launch date if needed
         */
        val initDate = Parameters.getInstance(applicationContext).getLong(ParameterKeys.INIT_DATE, 0)
        if (initDate <= 0) {
            Logger.debug("Registering first launch date")

            Parameters.getInstance(applicationContext).putLong(ParameterKeys.INIT_DATE, Date().time)
            CurrencyHelper.setUserCurrency(this, Currency.getInstance(Locale.getDefault())) // Set a default currency before onboarding
        }

        /*
         * Create local ID if needed
         */
        var localId = Parameters.getInstance(applicationContext).getString(ParameterKeys.LOCAL_ID)
        if (localId == null) {
            localId = UUID.randomUUID().toString()
            Logger.debug("Generating local id : $localId")

            Parameters.getInstance(applicationContext).putString(ParameterKeys.LOCAL_ID, localId)
        } else {
            Logger.debug("Local id : $localId")
        }

        // Activity counter for app foreground & background
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activityCounter = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityStarted(activity: Activity) {
                if (activityCounter == 0) {
                    onAppForeground(activity)
                }

                activityCounter++
            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {
                if (activityCounter == 1) {
                    onAppBackground()
                }

                activityCounter--
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }
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

            val dailyOpens = Parameters.getInstance(applicationContext).getInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, 0)
            if (dailyOpens > 2) {
                if (!hasRatingPopupBeenShownToday()) {
                    val shown = RatingPopup(activity).show(false)
                    if (shown) {
                        Parameters.getInstance(applicationContext).putLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, Date().time)
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

            if (Parameters.getInstance(applicationContext).getBoolean(ParameterKeys.PREMIUM_POPUP_COMPLETE, false)) {
                return
            }

            if ( iab.isUserPremium() ) {
                return
            }

            if (!UserHelper.hasUserCompleteRating(activity)) {
                return
            }

            val currentStep = RatingPopup.getUserStep(activity)
            if (currentStep == RatingPopup.RatingPopupStep.STEP_LIKE ||
                currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_NOT_RATED ||
                currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_RATED) {
                if (!hasRatingPopupBeenShownToday() && shouldShowPremiumPopup()) {
                    Parameters.getInstance(applicationContext).putLong(ParameterKeys.PREMIUM_POPUP_LAST_AUTO_SHOW, Date().time)

                    val dialog = AlertDialog.Builder(activity)
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
                            Parameters.getInstance(applicationContext).putBoolean(ParameterKeys.PREMIUM_POPUP_COMPLETE, true)
                            dialog1.dismiss()
                        }
                        .show()

                    UIHelper.centerDialogButtons(dialog)
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
        val lastRatingTS = Parameters.getInstance(applicationContext).getLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, 0)
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
        val lastPremiumTS = Parameters.getInstance(applicationContext).getLong(ParameterKeys.PREMIUM_POPUP_LAST_AUTO_SHOW, 0)
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
     * Show the 1.5 app update notification
     */
    private fun show1_5UpdateNotif() {
        val notifBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_NEW_FEATURES)
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.recurring_update_notification))
            .setStyle(NotificationCompat.BigTextStyle().bigText(resources.getString(R.string.recurring_update_notification)))
            .setColor(ContextCompat.getColor(applicationContext, R.color.accent))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT))

        NotificationManagerCompat.from(applicationContext).notify(4014, notifBuilder.build())
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

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityStarted(activity: Activity) {
                Batch.onStart(activity)
            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {
                Batch.onStop(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                Batch.onDestroy(activity)
            }
        })
    }

    /**
     * Check if a an update occured and call [.onUpdate] if so
     */
    private fun checkUpdateAction() {
        val savedVersion = Parameters.getInstance(applicationContext).getInt(ParameterKeys.APP_VERSION, 0)
        if (savedVersion > 0 && savedVersion != BuildConfig.VERSION_CODE) {
            onUpdate(savedVersion, BuildConfig.VERSION_CODE)
        }

        Parameters.getInstance(applicationContext).putInt(ParameterKeys.APP_VERSION, BuildConfig.VERSION_CODE)
    }

    /**
     * Called when an update occurred
     */
    private fun onUpdate(previousVersion: Int, newVersion: Int) {
        Logger.debug("Update detected, from $previousVersion to $newVersion")

        if (newVersion == BuildVersion.VERSION_1_2) {
            if (iab.isUserPremium() && !DailyNotifOptinService.hasDailyReminderOptinNotifBeenShown(this)) {
                DailyNotifOptinService.showDailyReminderOptinNotif(applicationContext)
            }
        }

        if (newVersion == BuildVersion.VERSION_1_3 && !MonthlyReportNotifService.hasUserSeenMonthlyReportNotif(this)) {
            if (iab.isUserPremium()) {
                MonthlyReportNotifService.showPremiumNotif(applicationContext)
            } else {
                MonthlyReportNotifService.showNotPremiumNotif(applicationContext)
            }
        }

        if (newVersion == BuildVersion.VERSION_1_5_2) {
            show1_5UpdateNotif()
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
        Parameters.getInstance(applicationContext).putInt(ParameterKeys.NUMBER_OF_OPEN, Parameters.getInstance(applicationContext).getInt(ParameterKeys.NUMBER_OF_OPEN, 0) + 1)

        /*
         * Check if last open is from another day
         */
        var shouldIncrementDailyOpen = false

        val lastOpen = Parameters.getInstance(applicationContext).getLong(ParameterKeys.LAST_OPEN_DATE, 0)
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
            Parameters.getInstance(applicationContext).putInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, Parameters.getInstance(applicationContext).getInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, 0) + 1)
        }

        /*
         * Save last open date
         */
        Parameters.getInstance(applicationContext).putLong(ParameterKeys.LAST_OPEN_DATE, Date().time)

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



    /**
     * Launch the redeem promocode flow
     *
     * @param promocode the promocode to redeem
     * @param activity the current activity
     */
    fun launchRedeemPromocodeFlow(promocode: String, activity: Activity): Boolean {
        try {
            val url = "https://play.google.com/redeem?code=" + URLEncoder.encode(promocode, "UTF-8")
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return true
        } catch (e: Exception) {
            Logger.error(false, "Error while redeeming promocode", e)
            return false
        }

    }
}
