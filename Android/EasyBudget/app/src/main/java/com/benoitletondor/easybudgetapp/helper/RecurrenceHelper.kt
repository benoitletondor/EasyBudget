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

import android.content.Context
import biweekly.util.Frequency
import biweekly.util.Recurrence
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType

fun Recurrence.toRecurringExpenseType(): RecurringExpenseType {
    val recurrenceInterval = interval
    return when(val recurrenceFrequency = frequency) {
        Frequency.DAILY -> RecurringExpenseType.DAILY
        Frequency.WEEKLY -> when(recurrenceInterval) {
            1 -> RecurringExpenseType.WEEKLY
            2 -> RecurringExpenseType.BI_WEEKLY
            3 -> RecurringExpenseType.TER_WEEKLY
            4 -> RecurringExpenseType.FOUR_WEEKLY
            else -> throw IllegalStateException("Recurrence interval not handled $recurrenceInterval for frequency: $recurrenceFrequency")
        }
        Frequency.MONTHLY -> when(recurrenceInterval) {
            1 -> RecurringExpenseType.MONTHLY
            2 -> RecurringExpenseType.BI_MONTHLY
            3 -> RecurringExpenseType.TER_MONTHLY
            6 -> RecurringExpenseType.SIX_MONTHLY
            else -> throw IllegalStateException("Recurrence interval not handled $recurrenceInterval for frequency: $recurrenceFrequency")
        }
        Frequency.YEARLY -> RecurringExpenseType.YEARLY

        Frequency.SECONDLY,
        Frequency.MINUTELY,
        Frequency.HOURLY,
        null -> throw IllegalStateException("Frequency not handled: $recurrenceFrequency")
    }
}

fun RecurringExpenseType.toRecurrence(): Recurrence {
    val (frequency, interval) = when(this) {
        RecurringExpenseType.DAILY -> Pair(Frequency.DAILY, 1)
        RecurringExpenseType.WEEKLY -> Pair(Frequency.WEEKLY, 1)
        RecurringExpenseType.BI_WEEKLY -> Pair(Frequency.WEEKLY, 2)
        RecurringExpenseType.TER_WEEKLY -> Pair(Frequency.WEEKLY, 3)
        RecurringExpenseType.FOUR_WEEKLY -> Pair(Frequency.WEEKLY, 4)
        RecurringExpenseType.MONTHLY -> Pair(Frequency.MONTHLY, 1)
        RecurringExpenseType.BI_MONTHLY -> Pair(Frequency.MONTHLY, 2)
        RecurringExpenseType.TER_MONTHLY -> Pair(Frequency.MONTHLY, 3)
        RecurringExpenseType.SIX_MONTHLY -> Pair(Frequency.MONTHLY, 6)
        RecurringExpenseType.YEARLY -> Pair(Frequency.YEARLY, 1)
    }

    return Recurrence.Builder(frequency).interval(interval).build()
}

fun RecurringExpenseType.toFormattedString(context: Context): String = when(this) {
    RecurringExpenseType.DAILY -> context.getString(R.string.daily)
    RecurringExpenseType.WEEKLY -> context.getString(R.string.weekly)
    RecurringExpenseType.BI_WEEKLY -> context.getString(R.string.bi_weekly)
    RecurringExpenseType.TER_WEEKLY -> context.getString(R.string.ter_weekly)
    RecurringExpenseType.FOUR_WEEKLY -> context.getString(R.string.four_weekly)
    RecurringExpenseType.MONTHLY -> context.getString(R.string.monthly)
    RecurringExpenseType.BI_MONTHLY -> context.getString(R.string.bi_monthly)
    RecurringExpenseType.TER_MONTHLY -> context.getString(R.string.ter_monthly)
    RecurringExpenseType.SIX_MONTHLY ->context.getString(R.string.six_monthly)
    RecurringExpenseType.YEARLY -> context.getString(R.string.yearly)
}