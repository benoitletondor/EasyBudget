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

import java.time.LocalDate
import java.util.ArrayList

/**
 * Get the list of months available for the user for the monthly report view.
 *
 * @return a list of Date object set at the 1st day of the month 00:00:00:000
 */
fun Parameters.getListOfMonthsAvailableForUser(): List<LocalDate> {
    val initDate = getInitDate() ?: return emptyList()
    val today = LocalDate.now()

    val months = ArrayList<LocalDate>()
    var currentDate = LocalDate.of(initDate.year, initDate.month, 1)

    while (currentDate.isBefore(today) || currentDate == today) {
        months.add(currentDate)
        currentDate = currentDate.plusMonths(1)
    }

    return months
}

fun LocalDate.computeCalendarMinDateFromInitDate(): LocalDate
    = minusYears(1)
