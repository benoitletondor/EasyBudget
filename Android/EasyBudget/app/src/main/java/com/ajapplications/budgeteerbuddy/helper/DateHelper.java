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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.ajapplications.budgeteerbuddy.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper to work with dates
 *
 * @author Benoit LETONDOR
 */
public class DateHelper
{
    /**
     * Remove hour, minutes, seconds and ms data from a date.
     *
     * @param date
     * @return a new cleaned date
     */
    public static Date cleanDate(@NonNull Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        return cal.getTime();
    }

    /**
     * Get the timestamp range for a given day starting at GMT - 11 finishing at GMT + 12
     *
     * @param date the day
     * @return a range of timestamps
     */
    public static Pair<Long, Long> getTimestampRangeForDay(@NonNull Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        cal.add(Calendar.HOUR_OF_DAY, -11);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, 23);
        long end = cal.getTimeInMillis();

        return new Pair<>(start, end);
    }

    /**
     * Remove hour, minutes, seconds and ms data from a date and return its GMT value
     *
     * @param date
     * @return a cleaned value of this date at GMT
     */
    public static Date cleanGMTDate(@NonNull Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        return cal.getTime();
    }

    /**
     * Get the list of months available for the user for the monthly report view.
     *
     * @param context non null context
     * @return a list of Date object set at the 1st day of the month 00:00:00:000
     */
    public static List<Date> getListOfMonthsAvailableForUser(@NonNull Context context)
    {
        long initDate = Parameters.getInstance(context).getLong(ParameterKeys.INIT_DATE, System.currentTimeMillis());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initDate);

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Date today = new Date();

        List<Date> months = new ArrayList<>();

        while( cal.getTime().before(today) )
        {
            months.add(cal.getTime());
            cal.add(Calendar.MONTH, 1);
        }

        return months;
    }

    /**
     * Get the title of the month to display in the report view
     *
     * @param context non null context
     * @param date date of the month
     * @return a formatted string like "January 2016"
     */
    public static String getMonthTitle(@NonNull Context context, @NonNull Date date)
    {
        SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.monthly_report_month_title_format), Locale.getDefault());
        return format.format(date);
    }
}
