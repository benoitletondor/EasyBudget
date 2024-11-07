package com.benoitletondor.easybudgetapp

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.benoitletondor.easybudgetapp.helper.Logger
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppUpdateManager(
    @ApplicationContext context: Context,
) : DefaultLifecycleObserver, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    fun plugToActivityLifecycle(activity: AppCompatActivity) {
        activity.lifecycle.addObserver(this)
    }

    fun unplugFromActivityLifecycle(activity: AppCompatActivity) {
        activity.lifecycle.removeObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        launch {
            try {
                val activity = owner as? Activity ?: throw IllegalStateException("Owner is not an activity")

                val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
                when(appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        Logger.debug("Update available")

                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            withContext(Dispatchers.Main) {
                                appUpdateManager.startUpdateFlow(
                                    appUpdateInfo,
                                    activity,
                                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                                ).await()
                            }
                        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            withContext(Dispatchers.Main) {
                                appUpdateManager.startUpdateFlow(
                                    appUpdateInfo,
                                    activity,
                                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                ).await()
                            }
                        }
                    }
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        Logger.debug("Update in progress, resuming")

                        withContext(Dispatchers.Main) {
                            appUpdateManager.startUpdateFlow(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                            ).await()
                        }
                    }
                    UpdateAvailability.UNKNOWN -> {
                        Logger.debug("Unknown update availability")
                    }
                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                        Logger.debug("No update available")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while checking for app update", e)
            }
        }
    }
}