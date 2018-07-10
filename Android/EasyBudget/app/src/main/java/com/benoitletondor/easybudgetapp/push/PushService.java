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

package com.benoitletondor.easybudgetapp.push;

import com.batch.android.Batch;
import com.benoitletondor.easybudgetapp.BuildConfig;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.helper.UserHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Calendar;
import java.util.Date;

/**
 * Service that handles Batch pushes
 *
 * @author Benoit LETONDOR
 */
public class PushService extends FirebaseMessagingService
{
    /**
     * Key to retrieve the max version for a push
     */
    private final static String INTENT_MAX_VERSION_KEY = "maxVersion";
    /**
     * Key to retrieve the max version for a push
     */
    private final static String INTENT_MIN_VERSION_KEY = "minVersion";
    /**
     * Key to retrieve if a push is intented for premium user or not
     */
    private final static String INTENT_PREMIUM_KEY = "premium";
    /**
     * Key to retrieve the daily reminder key for a push
     */
    private final static String DAILY_REMINDER_KEY = "daily";
    /**
     * Key to retrieve the monthly reminder key for a push
     */
    private final static String MONTHLY_REMINDER_KEY = "monthly";

// ----------------------------------->

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        super.onMessageReceived(remoteMessage);

        if (Batch.Push.shouldDisplayPush(this, remoteMessage)) // Check that the push is valid
        {
            if( !shouldDisplayPush(remoteMessage) )
            {
                Logger.debug("Not displaying push cause conditions are not matching");
                return;
            }

            // Display the notification
            Batch.Push.displayNotification(this, remoteMessage);
        }
    }

    /**
     * Check if the push should be displayed
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private boolean shouldDisplayPush(RemoteMessage remoteMessage)
    {
        return isUserOk(remoteMessage) && isVersionCompatible(remoteMessage) && isPremiumCompatible(remoteMessage);
    }

    /**
     * Check if the push should be displayed according to user choice
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private boolean isUserOk(RemoteMessage remoteMessage)
    {
        try
        {
            // Check if it's a daily reminder
            if( remoteMessage.getData().containsKey(DAILY_REMINDER_KEY) && "true".equals(remoteMessage.getData().get(DAILY_REMINDER_KEY)) )
            {
                if( !UserHelper.isUserPremium(getApplication()) ) // Only for premium users
                {
                    return false;
                }

                if( !UserHelper.isUserAllowingDailyReminderPushes(this) ) // Check user choice
                {
                    return false;
                }

                // Check if the app hasn't been opened today
                long lastOpenTimestamp = Parameters.getInstance(this).getLong(ParameterKeys.LAST_OPEN_DATE, 0);
                if( lastOpenTimestamp == 0 )
                {
                    return false;
                }

                Date lastOpen = new Date(lastOpenTimestamp);

                Calendar cal = Calendar.getInstance();
                int currentDay = cal.get(Calendar.DAY_OF_YEAR);
                cal.setTime(lastOpen);
                int lastOpenDay = cal.get(Calendar.DAY_OF_YEAR);

                return currentDay != lastOpenDay;
            }
            else if( remoteMessage.getData().containsKey(MONTHLY_REMINDER_KEY) && "true".equals(remoteMessage.getData().get(MONTHLY_REMINDER_KEY)) )
            {
                return UserHelper.isUserPremium(getApplication()) && UserHelper.isUserAllowingMonthlyReminderPushes(this);
            }

            // Else it must be an update push
            return UserHelper.isUserAllowingUpdatePushes(this);
        }
        catch (Exception e)
        {
            Logger.error("Error while checking user ok for push", e);
            return false;
        }
    }

    /**
     * Check if the push should be displayed according to version constrains
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private boolean isVersionCompatible(RemoteMessage remoteMessage)
    {
        try
        {
            int maxVersion = BuildConfig.VERSION_CODE;
            int minVersion = 1;

            if( remoteMessage.getData().containsKey(INTENT_MAX_VERSION_KEY) )
            {
                maxVersion = Integer.parseInt(remoteMessage.getData().get(INTENT_MAX_VERSION_KEY));
            }

            if( remoteMessage.getData().containsKey(INTENT_MIN_VERSION_KEY) )
            {
                minVersion = Integer.parseInt(remoteMessage.getData().get(INTENT_MIN_VERSION_KEY));
            }

            return BuildConfig.VERSION_CODE <= maxVersion && BuildConfig.VERSION_CODE >= minVersion;
        }
        catch(Exception e)
        {
            Logger.error("Error while checking app version for push", e);
            return false;
        }
    }

    /**
     * Check the user status if a push is marked as for premium or not.
     *
     * @param remoteMessage push intent
     * @return true if compatible, false otherwise
     */
    private boolean isPremiumCompatible(RemoteMessage remoteMessage)
    {
        try
        {
            if( remoteMessage.getData().containsKey(INTENT_PREMIUM_KEY) )
            {
                boolean isForPremium = "true".equals(remoteMessage.getData().get(INTENT_PREMIUM_KEY));

                return isForPremium == UserHelper.isUserPremium(getApplication());
            }

            return true;
        }
        catch (Exception e)
        {
            Logger.error("Error while checking premium compatible for push", e);
            return false;
        }
    }
}
