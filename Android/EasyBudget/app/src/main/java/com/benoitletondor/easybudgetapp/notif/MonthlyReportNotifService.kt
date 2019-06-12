/*
 *   Copyright 2016 Benoit LETONDOR
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
import com.benoitletondor.easybudgetapp.helper.ParameterKeys
import com.benoitletondor.easybudgetapp.helper.Parameters
import com.benoitletondor.easybudgetapp.view.main.MainActivity

import com.benoitletondor.easybudgetapp.notif.NotificationsChannels.CHANNEL_NEW_FEATURES

/**
 * Service and helper to build the monthly report notification shown to user on 1.3 update.
 *
 * @author Benoit LETONDOR
 */
// -------------------------------------->

class MonthlyReportNotifService : IntentService("MonthlyReportNotifService") {

    override fun onHandleIntent(intent: Intent?) {
        if( intent == null ) {
            return
        }

        try {
            if (INTENT_ACTION_DISCOVER == intent.action) {
                val notificationIntent = Intent(this, MainActivity::class.java)
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                notificationIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_PREMIUM_EXTRA, true)

                startActivity(notificationIntent)
            }
        } catch (e: Exception) {
            Logger.error("Error on non premium monthly report notif intent", e)
        }

        NotificationManagerCompat.from(this).cancel(NOT_PREMIUM_NOTIFICATION_ID)
    }

    companion object {
        /**
         * Id of the notification that must be used to display premium notif
         */
        private const val PREMIUM_NOTIFICATION_ID = 10043
        /**
         * Id of the notification that must be used to display not premium notif
         */
        private const val NOT_PREMIUM_NOTIFICATION_ID = 10044

        /**
         * Action for the intent that specifies user doesn't want to discover premium
         */
        private const val INTENT_ACTION_NOT_NOW = "intent.action.notnow"
        /**
         * Action for the intent that specifies user wants to discover premium
         */
        private const val INTENT_ACTION_DISCOVER = "intent.action.discover"

        /**
         * Show the notif to premium users about monthly report.
         *
         * @param context non null context
         */
        @JvmStatic
        fun showPremiumNotif(context: Context, parameters: Parameters) {
            val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val intent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notifBuilder = NotificationCompat.Builder(context, CHANNEL_NEW_FEATURES)
                .setSmallIcon(R.drawable.ic_push)
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setContentText(context.resources.getString(R.string.monthly_report_notif_premium_text))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.resources.getString(R.string.monthly_report_notif_premium_text)))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.accent))

            NotificationManagerCompat.from(context).notify(PREMIUM_NOTIFICATION_ID, notifBuilder.build())

            setUserSawMonthlyReportNotif(parameters)
        }

        /**
         * Show the notif to non premium users about monthly report.
         *
         * @param context non null context
         */
        @JvmStatic
        fun showNotPremiumNotif(context: Context, parameters: Parameters) {
            val notNowIntent = Intent(context, MonthlyReportNotifService::class.java)
            notNowIntent.action = INTENT_ACTION_NOT_NOW
            val notNowPendingIntent = PendingIntent.getService(context, 0, notNowIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val discoverIntent = Intent(context, MonthlyReportNotifService::class.java)
            discoverIntent.action = INTENT_ACTION_DISCOVER
            val discoverPendingIntent = PendingIntent.getService(context, 0, discoverIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notifBuilder = NotificationCompat.Builder(context, CHANNEL_NEW_FEATURES)
                .setSmallIcon(R.drawable.ic_push)
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setContentText(context.resources.getString(R.string.monthly_report_notif_notpremium_text))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.resources.getString(R.string.monthly_report_notif_notpremium_text)))
                .setContentIntent(discoverPendingIntent)
                .addAction(R.drawable.ic_do_not_disturb, context.resources.getString(R.string.premium_popup_become_not_now), notNowPendingIntent)
                .addAction(R.drawable.ic_search, context.resources.getString(R.string.premium_popup_become_cta), discoverPendingIntent)
                .setColor(ContextCompat.getColor(context, R.color.accent))

            NotificationManagerCompat.from(context).notify(NOT_PREMIUM_NOTIFICATION_ID, notifBuilder.build())

            setUserSawMonthlyReportNotif(parameters)
        }

    // -------------------------------------->

        /**
         * Has the user already saw the monthly report notification.
         *
         * @return true if user saw it, false otherwise
         */
        fun hasUserSeenMonthlyReportNotif(parameters: Parameters): Boolean {
            return parameters.getBoolean(ParameterKeys.MONTHLY_PUSH_NOTIF_SHOWN, false)
        }

        /**
         * Set that the user saw the monthly report notification.
         */
        private fun setUserSawMonthlyReportNotif(parameters: Parameters) {
            parameters.putBoolean(ParameterKeys.MONTHLY_PUSH_NOTIF_SHOWN, true)
        }
    }
}
