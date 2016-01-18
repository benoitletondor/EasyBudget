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

package com.benoitletondor.easybudgetapp.notif;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.view.MainActivity;

/**
 * Helper to build the monthly report notification shown to user on 1.3 update.
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportNotifHelper
{
    /**
     * Id of the notification that must be used to display premium notif
     */
    public final static int PREMIUM_NOTIFICATION_ID = 10043;
    /**
     * Id of the notification that must be used to display not premium notif
     */
    public final static int NOT_PREMIUM_NOTIFICATION_ID = 10044;

// -------------------------------------->

    /**
     * Show the notif to premium users about monthly report.
     *
     * @param context non null context
     */
    public static void showPremiumNotif(@NonNull Context context)
    {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(context.getResources().getString(R.string.app_name))
            .setContentText(context.getResources().getString(R.string.monthly_report_notif_premium_text))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.monthly_report_notif_premium_text)))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.accent));

        NotificationManagerCompat.from(context).notify(PREMIUM_NOTIFICATION_ID, notifBuilder.build());

        setUserSawMonthlyReportNotif(context);
    }

    /**
     * Show the notif to non premium users about monthly report.
     *
     * @param context non null context
     */
    public static void showNotPremiumNotif(@NonNull Context context)
    {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_PREMIUM_EXTRA, true);

        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(context.getResources().getString(R.string.app_name))
            .setContentText(context.getResources().getString(R.string.monthly_report_notif_notpremium_text))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.monthly_report_notif_notpremium_text)))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.accent));

        NotificationManagerCompat.from(context).notify(NOT_PREMIUM_NOTIFICATION_ID, notifBuilder.build());

        setUserSawMonthlyReportNotif(context);
    }

// -------------------------------------->

    /**
     * Has the user already saw the monthly report notification.
     *
     * @param context non null context
     * @return true if user saw it, false otherwise
     */
    public static boolean hasUserSeenMonthlyReportNotif(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.MONTHLY_PUSH_NOTIF_SHOWN, false);
    }

    /**
     * Set that the user saw the monthly report notification.
     *
     * @param context non null context
     */
    private static void setUserSawMonthlyReportNotif(@NonNull Context context)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.MONTHLY_PUSH_NOTIF_SHOWN, true);
    }
}
