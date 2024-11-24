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

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.batch.android.Batch
import com.batch.android.BatchActivityLifecycleHelper
import com.batch.android.BatchNotificationChannelsManager.DEFAULT_CHANNEL_ID
import com.batch.android.PushNotificationType
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.notif.*
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.push.PushService.Companion.DAILY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.push.PushService.Companion.MONTHLY_REMINDER_KEY
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import io.realm.kotlin.log.LogCategory
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.log.RealmLog
import io.realm.kotlin.log.RealmLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
@HiltAndroidApp
class EasyBudget : Application(), Configuration.Provider {
    @Inject lateinit var iab: Iab
    @Inject lateinit var parameters: Parameters
    @Inject lateinit var db: DB

    @Inject lateinit var workerFactory: HiltWorkerFactory

// ------------------------------------------>

    override fun onCreate() {
        super.onCreate()

        // Init actions
        init()

        // Crashlytics
        if ( BuildConfig.CRASHLYTICS_ACTIVATED ) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

            parameters.getLocalId()?.let {
                FirebaseCrashlytics.getInstance().setUserId(it)
            }
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }

        // Check if an update occurred and perform action if needed
        checkUpdateAction()

        // Ensure DB is created and reset init date if needed
        db.run {
            ensureDBCreated()

            // FIXME this should be done on restore, change that for the whole parameters restoration
            if( parameters.getShouldResetInitDate() ) {
                runBlocking { getOldestExpense() }?.let { expense ->
                    parameters.setInitDate(expense.date.toStartOfDayDate())
                }

                parameters.setShouldResetInitDate(false)
            }
        }

        // Batch
        setUpBatchSDK()

        // Realm
        setupRealm()

        // Setup theme
        AppCompatDelegate.setDefaultNightMode(parameters.getTheme().toPlatformValue())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Init app const and parameters
     *
     * DO NOT USE LOGGER HERE
     */
    private fun init() {
        /*
         * Save first launch date if needed
         */
        val initDate = parameters.getInitDate()
        if (initDate == null) {
            parameters.setInitDate(Date())

            // Set a default currency before onboarding
            val currency = try {
                Currency.getInstance(Locale.getDefault())
            } catch (e: Exception) {
                Currency.getInstance("USD")
            }

            parameters.setUserCurrency(currency)
        }

        /*
         * Create local ID if needed
         */
        var localId = parameters.getLocalId()
        if (localId == null) {
            localId = UUID.randomUUID().toString()
            parameters.setLocalId(localId)
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

        Batch.start(BuildConfig.BATCH_API_KEY)
        Batch.setFindMyInstallationEnabled(false)
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

    private fun setupRealm() {
        RealmLog.setLevel(level = if (BuildConfig.DEBUG_LOG) LogLevel.INFO else LogLevel.WARN)
        RealmLog.add(object : RealmLogger {
            override fun log(
                category: LogCategory,
                level: LogLevel,
                throwable: Throwable?,
                message: String?,
                vararg args: Any?
            ) {
                val argsString = args
                    .mapNotNull {
                        it?.toString()
                    }
                    .joinToString { ", " }

                when (level) {
                    LogLevel.WARN -> {
                        Logger.warning((message ?: "Realm warning") + " $argsString", throwable)
                    }
                    LogLevel.ERROR -> {
                        Logger.error((message ?: "Realm error") + " $argsString", throwable)
                    }

                    else -> Unit // No-op
                }
            }
        })
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

        if (previousVersion < 132) {
            GlobalScope.launch {
                try {
                    if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this@EasyBudget, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        return@launch
                    }

                    if (!iab.isUserPro()) {
                        return@launch
                    }

                    val intent = Intent(this@EasyBudget, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(this@EasyBudget, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                    val builder = NotificationCompat.Builder(this@EasyBudget, CHANNEL_NEW_FEATURES)
                        .setSmallIcon(R.drawable.ic_push)
                        .setContentTitle(getString(R.string.update_three_dot_two_notification_title))
                        .setContentText(getString(R.string.update_three_dot_two_notification_message))
                        .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.update_three_dot_two_notification_message)))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.ic_baseline_new_releases, getString(R.string.update_three_dot_two_notification_cta), pendingIntent)

                    with(NotificationManagerCompat.from(this@EasyBudget)) {
                        // notificationId is a unique int for each notification that you must define
                        notify(100001, builder.build())
                    }
                } catch (e: Exception) {
                    Logger.error("Error while showing update notification", e)
                }
            }
        }
    }

// -------------------------------------->

    /**
     * Called when the app goes foreground
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
         * Update iap status if needed
         */
        iab.updateIAPStatusIfNeeded()

        /*
         * Check if backup is late and trigger it if needed
         */
        checkBackupState(activity)
    }

    /**
     * Called when the app goes background
     */
    private fun onAppBackground() {
        Logger.debug("onAppBackground")
    }

    private fun checkBackupState(activity: Activity) {
        try {
            val componentActivity = activity as? ComponentActivity ?: throw IllegalStateException("Activity is not a ComponentActivity")

            componentActivity.lifecycleScope.launch(Dispatchers.IO) {
                iab.iabStatusFlow.collectLatest { iabStatusFlow ->
                    when(iabStatusFlow) {
                        PremiumCheckStatus.INITIALIZING,
                        PremiumCheckStatus.CHECKING,
                        PremiumCheckStatus.ERROR,
                        PremiumCheckStatus.NOT_PREMIUM -> Unit
                        PremiumCheckStatus.LEGACY_PREMIUM,
                        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                        PremiumCheckStatus.PRO_SUBSCRIBED -> {
                            if (parameters.isBackupEnabled()) {
                                fun backupDiffDays(lastBackupDate: Date?): Long? {
                                    if (lastBackupDate == null) {
                                        return null
                                    }

                                    val now = Date()
                                    val diff = now.time - lastBackupDate.time
                                    val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

                                    return diffInDays
                                }

                                val lastBackupDate = parameters.getLastBackupDate()
                                val backupDiffDaysValue = backupDiffDays(lastBackupDate)
                                if (backupDiffDaysValue != null && backupDiffDaysValue >= 14) {
                                    onBackupIsLate(backupDiffDaysValue)
                                } else {
                                    Logger.warning("Backup is active but never happened")
                                }
                            } else {
                                Logger.debug("Backup is inactive")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Logger.warning("Error while monitoring late offline account backup", e)
        }
    }

    private fun onBackupIsLate(noBackupSinceDays: Long) {
        Logger.warning("Backup is $noBackupSinceDays days late")

        val maybeLastManualRetriggerDate = parameters.getBackupManuallyRescheduledAt()
        if (maybeLastManualRetriggerDate != null) {
            val diff = Date().time - maybeLastManualRetriggerDate.time
            val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

            if (diffInDays < 5) {
                Logger.warning("Backup is $noBackupSinceDays days late but was manually retriggered $diffInDays days ago, ignoring")
                return
            }
        }

        unscheduleBackup(this)
        scheduleBackup(this)

        Logger.warning("Rescheduled backup", Exception("Backup is $noBackupSinceDays days late, rescheduled it"))
        parameters.setBackupManuallyRescheduledAt(Date())
    }

}
