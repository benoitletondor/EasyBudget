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

package com.benoitletondor.easybudgetapp.helper

import android.content.Context
import androidx.core.util.Pair

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitTimestamp

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Helper to work with dates
 *
 * @author Benoit LETONDOR
 */
object DateHelper {
    /**
     * Remove hour, minutes, seconds and ms data from a date.
     *
     * @return a new cleaned date
     */
    @JvmStatic
    fun cleanDate(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date

        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)

        return cal.time
    }

    /**
     * Get the timestamp range for a given day starting at GMT - 11 finishing at GMT + 12
     *
     * @param date the day
     * @return a range of timestamps
     */
    @JvmStatic
    fun getTimestampRangeForDay(date: Date): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.timeZone = TimeZone.getTimeZone("GMT")

        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)

        cal.add(Calendar.HOUR_OF_DAY, -11)
        val start = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 23)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    /**
     * Remove hour, minutes, seconds and ms data from a date and return its GMT value
     *
     * @return a cleaned value of this date at GMT
     */
    @JvmStatic
    fun cleanGMTDate(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.timeZone = TimeZone.getTimeZone("GMT")

        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)

        return cal.time
    }

    /**
     * Get the list of months available for the user for the monthly report view.
     *
     * @return a list of Date object set at the 1st day of the month 00:00:00:000
     */
    @JvmStatic
    fun getListOfMonthsAvailableForUser(parameters: Parameters): List<Date> {
        val initDate = parameters.getInitTimestamp()

        val cal = Calendar.getInstance()
        cal.timeInMillis = initDate

        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val today = Date()

        val months = ArrayList<Date>()

        while (cal.time.before(today)) {
            months.add(cal.time)
            cal.add(Calendar.MONTH, 1)
        }

        return months
    }

    /**
     * Get the title of the month to display in the report view
     *
     * @param context non null context
     * @param date date of the month
     * @return a formatted string like "January 2016"
     */
    @JvmStatic
    fun getMonthTitle(context: Context, date: Date): String {
        val format = SimpleDateFormat(context.resources.getString(R.string.monthly_report_month_title_format), Locale.getDefault())
        return format.format(date)
    }
}
