/*
 *   Copyright 2025 Benoit Letondor
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
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ListenableWorker
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.batch.android.Batch
import com.batch.android.BatchActivityLifecycleHelper
import com.batch.android.BatchNotificationChannelsManager.DEFAULT_CHANNEL_ID
import com.batch.android.PushNotificationType
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.notif.*
import com.benoitletondor.easybudgetapp.parameters.*
import com.benoitletondor.easybudgetapp.push.PushService.Companion.DAILY_REMINDER_KEY
import com.benoitletondor.easybudgetapp.push.PushService.Companion.MONTHLY_REMINDER_KEY
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
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
    @Inject lateinit var cloudStorage: CloudStorage
    @Inject lateinit var auth: Auth

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

        // Setup PowerSync
        setupPowerSync()

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
                    onAppForeground()
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

    private fun setupPowerSync() {
        co.touchlab.kermit.Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Debug else Severity.Info)
        co.touchlab.kermit.Logger.addLogWriter(object : LogWriter() {
            override fun log(
                severity: Severity,
                message: String,
                tag: String,
                throwable: Throwable?
            ) {
                if (severity === Severity.Error || severity === Severity.Assert) {
                    Logger.error("PowerSync","$tag: $message", throwable ?: Exception("PowerSync error: $message"))
                } else if (severity === Severity.Warn) {
                    Logger.warning("PowerSync","$tag: $message", throwable ?: Exception("PowerSync warning: $message"))
                } else if (severity === Severity.Info) {
                    Logger.debug("PowerSync","$tag: $message")
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
    private fun onAppForeground() {
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
        checkBackupState()
    }

    /**
     * Called when the app goes background
     */
    private fun onAppBackground() {
        Logger.debug("onAppBackground")

        offlineAccountBackupStatusWatchJob?.cancel()
    }

    private var offlineAccountBackupStatusWatchJob: Job? = null
    private fun checkBackupState() {
        try {
            offlineAccountBackupStatusWatchJob?.cancel()
            offlineAccountBackupStatusWatchJob = GlobalScope.launch(Dispatchers.IO) {
                getOfflineAccountBackupStatusFlow(iab, parameters, auth)
                    .collect { status ->
                        when(status) {
                            is OfflineAccountBackupStatus.Enabled -> {
                                val (authState, lastBackupDaysAgo) = status

                                when(authState) {
                                    is AuthState.Authenticated -> {
                                        if (lastBackupDaysAgo != null && lastBackupDaysAgo >= 14) {
                                            try {
                                                onBackupIsLate(lastBackupDaysAgo)
                                            } catch (e: Exception) {
                                                if (e is CancellationException) throw e

                                                Logger.error("Error while calling onBackupIsLate", e)
                                            }
                                        } else {
                                            Logger.warning("Backup is active but never happened")
                                        }
                                    }
                                    is AuthState.NotAuthenticated -> {
                                        Logger.warning("Backup is active but user is not authenticated", LateBackupWithUnauthUserException("Backup is active but user is not authenticated"))
                                    }
                                    is AuthState.Authenticating -> {
                                        // No-op
                                    }
                                }
                            }
                            OfflineAccountBackupStatus.Disabled -> {
                                Logger.debug("Backup is inactive")
                            }
                            OfflineAccountBackupStatus.Unavailable -> {
                                // No-op
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Logger.warning("Error while monitoring late offline account backup", e)
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun onBackupIsLate(noBackupSinceDays: Long) {
        Logger.warning("Backup is $noBackupSinceDays days late")

        val maybeLastManualRetriggerDate = parameters.getBackupManuallyRescheduledAt()
        if (maybeLastManualRetriggerDate != null) {
            val diff = Date().time - maybeLastManualRetriggerDate.time
            val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

            if (diffInDays < 5) {
                Logger.warning("Backup is $noBackupSinceDays days late but was manually retriggered $diffInDays days ago, ignoring")
            } else {
                Logger.warning(
                    "Backup is $noBackupSinceDays days late but was manually retriggered $diffInDays days ago, doing it manually and recheduling",
                    LateBackupWithManualRescheduleException("Backup is $noBackupSinceDays days late and has been recheduled $diffInDays days ago, rescheduled it"),
                )

                unscheduleBackup(this)
                scheduleBackup(this)

                parameters.setBackupManuallyRescheduledAt(Date())

                val result = backupDB(this, cloudStorage, auth, parameters, iab)
                if (result !is ListenableWorker.Result.Success) {
                    Logger.error("Error while performing manual backup: ${result.outputData}", LateBackupErrorWhileDoingManualBackup("Error while performing manual backup"))
                }
            }
        } else {
            unscheduleBackup(this)
            scheduleBackup(this)

            Logger.warning(
                "Rescheduled backup",
                LateBackupException("Backup is $noBackupSinceDays days late, rescheduled it"),
            )
            parameters.setBackupManuallyRescheduledAt(Date())
        }
    }

}

private class LateBackupException(message: String) : Exception(message)
private class LateBackupWithManualRescheduleException(message: String) : Exception(message)
private class LateBackupWithUnauthUserException(message: String) : Exception(message)
private class LateBackupErrorWhileDoingManualBackup(message: String) : Exception(message)
