package com.benoitletondor.easybudgetapp.config

import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.helper.Logger
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private const val REMOTE_CONFIG_FETCH_THROTTLE_DEFAULT_VALUE_HOURS = 1L

private const val GLOBAL_ALERT_MESSAGE_KEY = "global_alert_message"
private const val PRO_ALERT_MESSAGE_KEY = "pro_alert_message"

class FirebaseRemoteConfig : Config, ConfigUpdateListener, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private val remoteConfig = Firebase.remoteConfig

    private val globalAlertMessageMutableStateFlow = MutableStateFlow<String?>(null)
    private val proAlertMessageMutableStateFlow = MutableStateFlow<String?>(null)

    init {
        remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = if(BuildConfig.DEBUG) {
                0L
            } else {
                TimeUnit.HOURS.toSeconds(REMOTE_CONFIG_FETCH_THROTTLE_DEFAULT_VALUE_HOURS)
            }
        })

        launch {
            try {
                remoteConfig.fetchAndActivate().await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while fetching remote config", e)
            }

            remoteConfig.addOnConfigUpdateListener(this@FirebaseRemoteConfig)

            globalAlertMessageMutableStateFlow.value = remoteConfig.getStringOrNull(GLOBAL_ALERT_MESSAGE_KEY)
            proAlertMessageMutableStateFlow.value = remoteConfig.getStringOrNull(PRO_ALERT_MESSAGE_KEY)
        }
    }

    override fun onUpdate(configUpdate: ConfigUpdate) {
        launch {
            remoteConfig.activate().await()
            configUpdate.updatedKeys.forEach { key ->
                when(key) {
                    GLOBAL_ALERT_MESSAGE_KEY -> globalAlertMessageMutableStateFlow.value = remoteConfig.getStringOrNull(GLOBAL_ALERT_MESSAGE_KEY)
                    PRO_ALERT_MESSAGE_KEY -> proAlertMessageMutableStateFlow.value = remoteConfig.getStringOrNull(PRO_ALERT_MESSAGE_KEY)
                }
            }
        }
    }

    override fun onError(error: FirebaseRemoteConfigException) {
        Logger.error("Error while watching for remote config updates", error)
    }

    override fun watchGlobalAlertMessage(): StateFlow<String?> = globalAlertMessageMutableStateFlow

    override fun watchProAlertMessage(): StateFlow<String?> = proAlertMessageMutableStateFlow
}

private fun com.google.firebase.remoteconfig.FirebaseRemoteConfig.getStringOrNull(key: String): String? {
    val value = this.getString(key)
    return if (value.isNotEmpty()) {
        this.getString(key)
    } else {
        null
    }
}