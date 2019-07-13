/*
 *   Copyright 2019 Benoit LETONDOR
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

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitTimestamp

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Get the list of months available for the user for the monthly report view.
 *
 * @return a list of Date object set at the 1st day of the month 00:00:00:000
 */
fun Parameters.getListOfMonthsAvailableForUser(): List<Date> {
    val initDate = getInitTimestamp()

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
 * @return a formatted string like "January 2016"
 */
fun Date.getMonthTitle(context: Context): String {
    val format = SimpleDateFormat(context.resources.getString(R.string.monthly_report_month_title_format), Locale.getDefault())
    return format.format(this)
}
