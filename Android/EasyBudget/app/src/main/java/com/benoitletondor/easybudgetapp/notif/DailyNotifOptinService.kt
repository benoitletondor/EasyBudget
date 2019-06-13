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

package com.benoitletondor.easybudgetapp.notif

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.setUserAllowDailyReminderPushes
import com.benoitletondor.easybudgetapp.view.main.MainActivity

import com.benoitletondor.easybudgetapp.notif.NotificationsChannels.CHANNEL_NEW_FEATURES
import org.koin.java.KoinJavaComponent.get

/**
 * Service that will receive events from the daily reminder opt-in notification
 *
 * @author Benoit LETONDOR
 */
class DailyNotifOptinService : IntentService("DailyNotifOptinService") {

    private val parameters = get(Parameters::class.java)

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            Logger.error("DailyNotifOptinService launched with null intent, abort")
            return
        }

        Logger.debug("DailyNotifOptinService: received intent: " + intent.action!!)

        when {
            INTENT_OPTOUT_ACTION == intent.action -> parameters.setUserAllowDailyReminderPushes(false)
            INTENT_OPTIN_ACTION == intent.action -> parameters.setUserAllowDailyReminderPushes(true)
            INTENT_REDIRECT_ACTION == intent.action -> {
                parameters.setUserAllowDailyReminderPushes(true)

                val openSettingsIntent = Intent(this, MainActivity::class.java)
                openSettingsIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_SETTINGS_EXTRA, true)
                openSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(openSettingsIntent)
            }
        }

        NotificationManagerCompat.from(this).cancel(OPTIN_NOTIFICATION_ID)
    }

    companion object {
        /**
         * Id of the notification that must be used to display the notif (so that the service can
         * dismiss it)
         */
        private const val OPTIN_NOTIFICATION_ID = 10042
        /**
         * Intent that specify the user opt-out from daily reminder notification
         */
        private const val INTENT_OPTOUT_ACTION = "daily_reminder_optout"
        /**
         * Intent that specify the user opt-in from daily reminder notification
         */
        private const val INTENT_OPTIN_ACTION = "daily_reminder_optin"
        /**
         * Intent that is broadcasted when the user clicked on the notif. Redirects him to the settings.
         */
        private const val INTENT_REDIRECT_ACTION = "daily_reminder_redirect"

     // ---------------------------------------->

        /**
         * Show the daily reminder optin notification to the user
         *
         * @param context a non null context
         */
        @JvmStatic
        fun showDailyReminderOptinNotif(context: Context, parameters: Parameters) {
            try {
                val optoutIntent = Intent(context, DailyNotifOptinService::class.java)
                optoutIntent.action = INTENT_OPTOUT_ACTION
                val optoutPendingIntent = PendingIntent.getService(context, 0, optoutIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val optinIntent = Intent(context, DailyNotifOptinService::class.java)
                optinIntent.action = INTENT_OPTIN_ACTION
                val optinPendingIntent = PendingIntent.getService(context, 0, optinIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val redirectIntent = Intent(context, DailyNotifOptinService::class.java)
                redirectIntent.action = INTENT_REDIRECT_ACTION
                val redirectPendingIntent = PendingIntent.getService(context, 0, redirectIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notifBuilder = NotificationCompat.Builder(context, CHANNEL_NEW_FEATURES)
                    .setSmallIcon(R.drawable.ic_push)
                    .setContentTitle(context.resources.getString(R.string.app_name))
                    .setContentText(context.resources.getString(R.string.premium_notif_optin_message))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(context.resources.getString(R.string.premium_notif_optin_message)))
                    .addAction(R.drawable.ic_do_not_disturb, context.resources.getString(R.string.premium_notif_optin_message_not_ok), optoutPendingIntent)
                    .addAction(R.drawable.ic_thumb_up, context.resources.getString(R.string.premium_notif_optin_message_ok), optinPendingIntent)
                    .setContentIntent(redirectPendingIntent)
                    .setDeleteIntent(optoutPendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.accent))

                NotificationManagerCompat.from(context).notify(OPTIN_NOTIFICATION_ID, notifBuilder.build())

                parameters.setDailyReminderOptinNotifShown()
            } catch (e: Exception) {
                Logger.error("Error while showing daily notif optin notif", e)
            }
        }
    }
}

/**
 * Has the daily push opt-in been shown to the user yet (bool)
 */
private const val DAILY_PUSH_NOTIF_SHOWN_PARAMETERS_KEY = "user_saw_daily_push_notif"

private fun Parameters.setDailyReminderOptinNotifShown() {
    putBoolean(DAILY_PUSH_NOTIF_SHOWN_PARAMETERS_KEY, true)
}

/**
 * Has the daily reminder opt-in notification already been shown
 *
 * @return true if already shown, false otherwise
 */
fun Parameters.hasDailyReminderOptinNotifBeenShow(): Boolean {
    return getBoolean(DAILY_PUSH_NOTIF_SHOWN_PARAMETERS_KEY, false)
}