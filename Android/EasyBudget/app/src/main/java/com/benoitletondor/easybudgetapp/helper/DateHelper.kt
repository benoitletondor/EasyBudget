/*
 *   Copyright 2024 Benoit LETONDOR
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

import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.kizitonwose.calendar.core.yearMonth

import java.time.LocalDate
import java.time.YearMonth
import java.util.ArrayList

/**
 * Get the list of months available in the monthly report view.
 */
fun Parameters.getListOfMonthsAvailableForUser(): List<YearMonth> {
    val initDate = getInitDate() ?: return emptyList()
    val endRange = LocalDate.now().yearMonth.plusMonths(13)

    val months = ArrayList<YearMonth>()
    var currentMonth = initDate.yearMonth

    while (currentMonth.isBefore(endRange)) {
        months.add(currentMonth)
        currentMonth = currentMonth.plusMonths(1)
    }

    return months
}

fun LocalDate.computeCalendarMinDateFromInitDate(): LocalDate
    = minusYears(1)
