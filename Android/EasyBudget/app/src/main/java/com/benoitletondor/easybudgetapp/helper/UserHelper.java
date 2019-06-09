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

package com.benoitletondor.easybudgetapp.helper;

import androidx.annotation.NonNull;
import com.roomorama.caldroid.CaldroidFragment;

import static com.benoitletondor.easybudgetapp.helper.ParameterKeys.FIRST_DAY_OF_WEEK;

/**
 * Helper to get user status / preferences
 *
 * @author Benoit LETONDOR
 */
public class UserHelper
{
    /**
     * The user wants or not to receive notification about updates
     *
     * @return true if we can display update notifications, false otherwise
     */
    public static boolean isUserAllowingUpdatePushes(@NonNull Parameters parameters)
    {
        return parameters.getBoolean(ParameterKeys.USER_ALLOW_UPDATE_PUSH, true);
    }

    /**
     * Set the user choice about update notifications
     *
     * @param value if the user wants or not to receive notifications about updates
     */
    public static void setUserAllowUpdatePushes(@NonNull Parameters parameters, boolean value)
    {
        parameters.putBoolean(ParameterKeys.USER_ALLOW_UPDATE_PUSH, value);
    }

    /**
     * The user wants or not to receive a daily reminder notification
     *
     * @return true if we can display daily notifications, false otherwise
     */
    public static boolean isUserAllowingDailyReminderPushes(@NonNull Parameters parameters)
    {
        return parameters.getBoolean(ParameterKeys.USER_ALLOW_DAILY_PUSH, true);
    }

    /**
     * Set the user choice about daily reminder notifications
     *
     * @param value if the user wants or not to receive daily notifications
     */
    public static void setUserAllowDailyReminderPushes(@NonNull Parameters parameters, boolean value)
    {
        parameters.putBoolean(ParameterKeys.USER_ALLOW_DAILY_PUSH, value);
    }

    /**
     * The user wants or not to receive a daily monthly notification when report is available
     *
     * @return true if we can display monthly notifications, false otherwise
     */
    public static boolean isUserAllowingMonthlyReminderPushes(@NonNull Parameters parameters)
    {
        return parameters.getBoolean(ParameterKeys.USER_ALLOW_MONTHLY_PUSH, true);
    }

    /**
     * Set the user choice about monthly reminder notifications
     *
     * @param value if the user wants or not to receive monthly notifications
     */
    public static void setUserAllowMonthlyReminderPushes(@NonNull Parameters parameters, boolean value)
    {
        parameters.putBoolean(ParameterKeys.USER_ALLOW_MONTHLY_PUSH, value);
    }

    /**
     * Has the user complete the rating popup
     *
     * @return true if the user has already answered, false otherwise
     */
    public static boolean hasUserCompleteRating(@NonNull Parameters parameters)
    {
        return parameters.getBoolean(ParameterKeys.RATING_COMPLETED, false);
    }

    /**
     * Set that the user has complete the rating popup process
     */
    public static void setUserHasCompleteRating(@NonNull Parameters parameters)
    {
        parameters.putBoolean(ParameterKeys.RATING_COMPLETED, true);
    }

    /**
     * Has the user saw the monthly report hint so far
     *
     * @return true if the user saw it, false otherwise
     */
    public static boolean hasUserSawMonthlyReportHint(@NonNull Parameters parameters)
    {
        return parameters.getBoolean(ParameterKeys.USER_SAW_MONTHLY_REPORT_HINT, false);
    }

    /**
     * Set that the user saw the monthly report hint
     */
    public static void setUserSawMonthlyReportHint(@NonNull Parameters parameters)
    {
        parameters.putBoolean(ParameterKeys.USER_SAW_MONTHLY_REPORT_HINT, true);
    }

    /**
     * Get the first day of the week to display to the user
     *
     * @return the id of the first day of week to display
     */
    public static int getFirstDayOfWeek(@NonNull Parameters parameters)
    {
        final int currentValue = parameters.getInt(FIRST_DAY_OF_WEEK, -1);
        if( currentValue < 1 || currentValue > 7 ) {
            return CaldroidFragment.MONDAY;
        }

        return currentValue;
    }

    /**
     * Set the first day of week to display to the user
     *
     * @param firstDayOfWeek the id of the first day of week to display
     */
    public static void setFirstDayOfWeek(@NonNull Parameters parameters, int firstDayOfWeek)
    {
        parameters.putInt(FIRST_DAY_OF_WEEK, firstDayOfWeek);
    }
}
