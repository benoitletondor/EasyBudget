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

package com.ajapplications.budgeteerbuddy.notif;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.ajapplications.budgeteerbuddy.helper.Logger;
import com.ajapplications.budgeteerbuddy.helper.ParameterKeys;
import com.ajapplications.budgeteerbuddy.helper.Parameters;
import com.ajapplications.budgeteerbuddy.R;
import com.ajapplications.budgeteerbuddy.view.MainActivity;

/**
 * Service and helper to build the monthly report notification shown to user on 1.3 update.
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportNotifService extends IntentService
{
    /**
     * Id of the notification that must be used to display premium notif
     */
    public final static int PREMIUM_NOTIFICATION_ID = 10043;
    /**
     * Id of the notification that must be used to display not premium notif
     */
    public final static int NOT_PREMIUM_NOTIFICATION_ID = 10044;

    /**
     * Action for the intent that specifies user doesn't want to discover premium
     */
    private final static String INTENT_ACTION_NOT_NOW = "intent.action.notnow";
    /**
     * Action for the intent that specifies user wants to discover premium
     */
    private final static String INTENT_ACTION_DISCOVER = "intent.action.discover";

// -------------------------------------->

    public MonthlyReportNotifService()
    {
        super("MonthlyReportNotifService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        try
        {
            if( INTENT_ACTION_DISCOVER.equals(intent.getAction()) )
            {
                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                notificationIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_PREMIUM_EXTRA, true);

                startActivity(notificationIntent);
            }
        }
        catch (Exception e)
        {
            Logger.error("Error on non premium monthly report notif intent", e);
        }

        NotificationManagerCompat.from(this).cancel(NOT_PREMIUM_NOTIFICATION_ID);
    }

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
        Intent notNowIntent = new Intent(context, MonthlyReportNotifService.class);
        notNowIntent.setAction(INTENT_ACTION_NOT_NOW);
        PendingIntent notNowPendingIntent = PendingIntent.getService(context, 0, notNowIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent discoverIntent = new Intent(context, MonthlyReportNotifService.class);
        discoverIntent.setAction(INTENT_ACTION_DISCOVER);
        PendingIntent discoverPendingIntent = PendingIntent.getService(context, 0, discoverIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(context.getResources().getString(R.string.app_name))
            .setContentText(context.getResources().getString(R.string.monthly_report_notif_notpremium_text))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.monthly_report_notif_notpremium_text)))
            .setContentIntent(discoverPendingIntent)
            .addAction(R.drawable.ic_do_not_disturb, context.getResources().getString(R.string.premium_popup_become_not_now), notNowPendingIntent)
            .addAction(R.drawable.ic_search, context.getResources().getString(R.string.premium_popup_become_cta), discoverPendingIntent)
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
