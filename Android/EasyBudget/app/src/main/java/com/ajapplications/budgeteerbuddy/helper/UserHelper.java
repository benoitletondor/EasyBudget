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

package com.ajapplications.budgeteerbuddy.helper;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Helper to get user status / preferences
 *
 * @author Benoit LETONDOR
 */
public class UserHelper
{
    /**
     * Feature reference for premium (used by Batch)
     */
    public static final String BATCH_PREMIUM_FEATURE = "PREMIUM";

// ------------------------------------>

    /**
     * Is the user a premium user
     *
     * @param application non null application
     * @return true if the user if premium, false otherwise
     */
    public static boolean isUserPremium(@NonNull Application application)
    {
        return Parameters.getInstance(application).getBoolean(ParameterKeys.PREMIUM, false) ||
                Parameters.getInstance(application).getBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, false) ||
                Parameters.getInstance(application).getBoolean(ParameterKeys.APP_TURBO_PREMIUM, false);
    }

    /**
     * Set this user as premium from Batch
     *
     * @param context non null context
     */
    public static void setBatchUserPremium(@NonNull Context context)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, true);
    }

    /**
     * The user wants or not to receive notification about updates
     *
     * @param context non null context
     * @return true if we can display update notifications, false otherwise
     */
    public static boolean isUserAllowingUpdatePushes(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.USER_ALLOW_UPDATE_PUSH, true);
    }

    /**
     * Set the user choice about update notifications
     *
     * @param context non null context
     * @param value if the user wants or not to receive notifications about updates
     */
    public static void setUserAllowUpdatePushes(@NonNull Context context, boolean value)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.USER_ALLOW_UPDATE_PUSH, value);
    }

    /**
     * The user wants or not to receive a daily reminder notification
     *
     * @param context non null context
     * @return true if we can display daily notifications, false otherwise
     */
    public static boolean isUserAllowingDailyReminderPushes(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.USER_ALLOW_DAILY_PUSH, true);
    }

    /**
     * Set the user choice about daily reminder notifications
     *
     * @param context non null context
     * @param value if the user wants or not to receive daily notifications
     */
    public static void setUserAllowDailyReminderPushes(@NonNull Context context, boolean value)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.USER_ALLOW_DAILY_PUSH, value);
    }

    /**
     * The user wants or not to receive a daily monthly notification when report is available
     *
     * @param context non null context
     * @return true if we can display monthly notifications, false otherwise
     */
    public static boolean isUserAllowingMonthlyReminderPushes(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.USER_ALLOW_MONTHLY_PUSH, true);
    }

    /**
     * Set the user choice about monthly reminder notifications
     *
     * @param context non null context
     * @param value if the user wants or not to receive monthly notifications
     */
    public static void setUserAllowMonthlyReminderPushes(@NonNull Context context, boolean value)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.USER_ALLOW_MONTHLY_PUSH, value);
    }

    /**
     * Has the user complete the rating popup
     *
     * @param context non null context
     * @return true if the user has already answered, false otherwise
     */
    public static boolean hasUserCompleteRating(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.RATING_COMPLETED, false);
    }

    /**
     * Set that the user has complete the rating popup process
     *
     * @param context non null context
     */
    public static void setUserHasCompleteRating(@NonNull Context context)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.RATING_COMPLETED, true);
    }

    /**
     * Has the user saw the monthly report hint so far
     *
     * @param context non null context
     * @return true if the user saw it, false otherwise
     */
    public static boolean hasUserSawMonthlyReportHint(@NonNull Context context)
    {
        return Parameters.getInstance(context).getBoolean(ParameterKeys.USER_SAW_MONTHLY_REPORT_HINT, false);
    }

    /**
     * Set that the user saw the monthly report hint
     *
     * @param context non null context
     */
    public static void setUserSawMonthlyReportHint(@NonNull Context context)
    {
        Parameters.getInstance(context).putBoolean(ParameterKeys.USER_SAW_MONTHLY_REPORT_HINT, true);
    }
}
