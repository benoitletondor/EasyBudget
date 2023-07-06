package com.benoitletondor.easybudgetapp.helper

import biweekly.util.Frequency
import biweekly.util.Recurrence
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