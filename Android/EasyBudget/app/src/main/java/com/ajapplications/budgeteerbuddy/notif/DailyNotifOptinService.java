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
import com.ajapplications.budgeteerbuddy.helper.UserHelper;
import com.ajapplications.budgeteerbuddy.R;
import com.ajapplications.budgeteerbuddy.view.MainActivity;

/**
 * Service that will receive events from the daily reminder opt-in notification
 *
 * @author Benoit LETONDOR
 */
public class DailyNotifOptinService extends IntentService
{
    /**
     * Id of the notification that must be used to display the notif (so that the service can
     * dismiss it)
     */
    public final static int OPTIN_NOTIFICATION_ID = 10042;
    /**
     * Intent that specify the user opt-out from daily reminder notification
     */
    public final static String INTENT_OPTOUT_ACTION = "daily_reminder_optout";
    /**
     * Intent that specify the user opt-in from daily reminder notification
     */
    public final static String INTENT_OPTIN_ACTION = "daily_reminder_optin";
    /**
     * Intent that is broadcasted when the user clicked on the notif. Redirects him to the settings.
     */
    public final static String INTENT_REDIRECT_ACTION = "daily_reminder_redirect";

// ---------------------------------------->

    public DailyNotifOptinService()
    {
        super("DailyNotifOptinService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if( intent == null )
        {
            Logger.error("DailyNotifOptinService launched with null intent, abort");
            return;
        }

        Logger.debug("DailyNotifOptinService: received intent: "+intent.getAction());

        if( INTENT_OPTOUT_ACTION.equals(intent.getAction()) )
        {
            UserHelper.setUserAllowDailyReminderPushes(this, false);
        }
        else if( INTENT_OPTIN_ACTION.equals(intent.getAction()) )
        {
            UserHelper.setUserAllowDailyReminderPushes(this, true);
        }
        else if( INTENT_REDIRECT_ACTION.equals(intent.getAction()) )
        {
            UserHelper.setUserAllowDailyReminderPushes(this, true);

            Intent openSettingsIntent = new Intent(this, MainActivity.class);
            openSettingsIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_SETTINGS_EXTRA, true);
            openSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(openSettingsIntent);
        }

        NotificationManagerCompat.from(this).cancel(OPTIN_NOTIFICATION_ID);
    }

// ---------------------------------------->

    /**
     * Show the daily reminder optin notification to the user
     *
     * @param context a non null context
     */
    public static void showDailyReminderOptinNotif(@NonNull Context context)
    {
        try
        {
            Intent optoutIntent = new Intent(context, DailyNotifOptinService.class);
            optoutIntent.setAction(DailyNotifOptinService.INTENT_OPTOUT_ACTION);
            PendingIntent optoutPendingIntent = PendingIntent.getService(context, 0, optoutIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent optinIntent = new Intent(context, DailyNotifOptinService.class);
            optinIntent.setAction(DailyNotifOptinService.INTENT_OPTIN_ACTION);
            PendingIntent optinPendingIntent = PendingIntent.getService(context, 0, optinIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent redirectIntent = new Intent(context, DailyNotifOptinService.class);
            redirectIntent.setAction(DailyNotifOptinService.INTENT_REDIRECT_ACTION);
            PendingIntent redirectPendingIntent = PendingIntent.getService(context, 0, redirectIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_push)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(context.getResources().getString(R.string.premium_notif_optin_message))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.premium_notif_optin_message)))
                .addAction(R.drawable.ic_do_not_disturb, context.getResources().getString(R.string.premium_notif_optin_message_not_ok), optoutPendingIntent)
                .addAction(R.drawable.ic_thumb_up, context.getResources().getString(R.string.premium_notif_optin_message_ok), optinPendingIntent)
                .setContentIntent(redirectPendingIntent)
                .setDeleteIntent(optoutPendingIntent)
                .setColor(ContextCompat.getColor(context, R.color.accent));

            NotificationManagerCompat.from(context).notify(OPTIN_NOTIFICATION_ID, notifBuilder.build());

            Parameters.getInstance(context).putBoolean(ParameterKeys.DAILY_PUSH_NOTIF_SHOWN, true);
        }
        catch (Exception e)
        {
            Logger.error("Error while showing daily notif optin notif", e);
        }
    }

    /**
     * Has the daily reminder opt-in notification already been shown
     *
     * @param context non null context
     * @return true if already shown, false otherwise
     */
    public static boolean hasDailyReminderOptinNotifBeenShown(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.DAILY_PUSH_NOTIF_SHOWN, false);
    }
}
