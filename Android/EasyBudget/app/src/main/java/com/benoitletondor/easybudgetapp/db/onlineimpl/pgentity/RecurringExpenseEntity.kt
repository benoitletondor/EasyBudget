/*
 *   Copyright 2025 Benoit Letondor
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

package com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.DateEnd
import biweekly.property.DateStart
import biweekly.property.RecurrenceId
import biweekly.property.RecurrenceRule
import biweekly.property.Status
import biweekly.property.Summary
import biweekly.util.ICalDate
import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.helper.toRecurrence
import com.benoitletondor.easybudgetapp.helper.toRecurringExpenseType
import com.benoitletondor.easybudgetapp.helper.toStartOfDayDate
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.powersync.db.SqlCursor
import com.powersync.db.internal.PowerSyncTransaction
import com.powersync.db.schema.Column
import com.powersync.db.schema.Index
import com.powersync.db.schema.IndexedColumn
import com.powersync.db.schema.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone

private const val AMOUNT_KEY = "amount"
private const val CHECKED_KEY = "checked"

const val RECURRING_EXPENSE_TABLE_NAME = "recurring_expense"
private const val RECURRING_EXPENSE_ID_COLUMN_INDEX = 0
private const val RECURRING_EXPENSE_ACCOUNT_ID_COLUMN_INDEX = 1
private const val RECURRING_EXPENSE_I_CAL_REPRESENTATION_COLUMN_INDEX = 2

val recurringExpenseEntityTable = Table(
    RECURRING_EXPENSE_TABLE_NAME,
    listOf(
        Column.text("account_id"),
        Column.text("i_cal_representation"),
        Column.text("created_at"),
    ),
    indexes = listOf(
        Index(
            name = "created_at_index",
            columns = listOf(IndexedColumn.ascending("created_at")),
        ),
    )
)

class RecurringExpenseEntity(
    val id: Long,
    val accountId: String,
    var iCalRepresentation: String,
) {
    suspend fun toRecurringExpense(): RecurringExpense {
        val event = getCal().events
            .filterExceptions()
            .last()

        val startDate = localDateFromTimestamp(event.dateStart.value.time)
        val title = event.summary.value
        val originalAmount = event.getExperimentalProperty(AMOUNT_KEY).value.toLong()
        val recurrenceExpenseType = event.recurrenceRule.value.toRecurringExpenseType()

        return RecurringExpense(
            id,
            title,
            originalAmount.getRealValueFromDB(),
            startDate,
            modified = false,
            recurrenceExpenseType,
        )
    }

    suspend fun generateExpenses(from: LocalDate?, to: LocalDate): List<Expense> {
        return getCal().getExpenses(from, to, toRecurringExpense())
    }

    suspend fun addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        val cal = getCal()
        cal.addExceptionFromExpense(expense, originalOccurrenceDate)
        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    private fun ICalendar.findNonExceptionEventForOriginalOccurrenceDate(originalOccurrenceDate: LocalDate): VEvent
            = events
        .filterExceptions()
        .let { nonExceptionEvents ->
            val firstNonExceptionEvent = nonExceptionEvents.firstOrNull {
                it.dateEnd == null || !it.dateEnd.value.before(originalOccurrenceDate.toStartOfDayDate())
            }

            if (firstNonExceptionEvent != null) {
                return@let firstNonExceptionEvent
            }

            Logger.warning("No non exception event found for original occurrence date $originalOccurrenceDate, using first event")
            return@let nonExceptionEvents.first { it.dateEnd !== null && !it.dateStart.value.after(originalOccurrenceDate.toStartOfDayDate()) }
        }

    private fun ICalendar.addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(expense.date.toStartOfDayDate(), false)
        exceptionEvent.summary = Summary(expense.title)
        exceptionEvent.status = Status.accepted()
        exceptionEvent.addExperimentalProperty(AMOUNT_KEY, expense.amount.getDBValue().toString())
        exceptionEvent.addExperimentalProperty(CHECKED_KEY, expense.checked.toString())
        exceptionEvent.uid = findNonExceptionEventForOriginalOccurrenceDate(originalOccurrenceDate).uid

        val recurrenceId = RecurrenceId(ICalDate(originalOccurrenceDate.toStartOfDayDate(), false))
        exceptionEvent.recurrenceId = recurrenceId

        // Cancel any previous exception
        events.filterNotException()
            .forEach { exception ->
                if (exception.recurrenceId.value.time == originalOccurrenceDate.toStartOfDayDate().time) {
                    exception.status.value = Status.CANCELLED
                }
            }

        // Add exception
        addEvent(exceptionEvent)
    }

    suspend fun deleteOccurrence(occurrenceDate: LocalDate, originalOccurrenceDate: LocalDate) {
        val cal = getCal()

        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(occurrenceDate.toStartOfDayDate(), false)
        exceptionEvent.uid = cal.findNonExceptionEventForOriginalOccurrenceDate(originalOccurrenceDate).uid
        exceptionEvent.status = Status.cancelled()
        val recurrenceId = RecurrenceId(ICalDate(originalOccurrenceDate.toStartOfDayDate(), false))
        exceptionEvent.recurrenceId = recurrenceId

        // Cancel any previous exception
        cal.events.filterNotException()
            .forEach { exception ->
                if (exception.recurrenceId.value.time == originalOccurrenceDate.toStartOfDayDate().time) {
                    exception.status.value = Status.CANCELLED
                }
            }

        cal.addEvent(exceptionEvent)

        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    suspend fun deleteOccurrencesAfterDate(date: LocalDate) {
        val cal = getCal()

        cal.events
            .filterExceptions()
            .forEach { event ->
                if (event.dateEnd == null || event.dateEnd.value.after(date.toStartOfDayDate())) {
                    event.dateEnd = DateEnd(date.toStartOfDayDate(), false)
                }
            }

        cal.events
            .filterNotException()
            .forEach { exception ->
                if (exception.dateStart.value.after(date.toStartOfDayDate())) {
                    exception.status.value = Status.CANCELLED
                }
            }

        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    suspend fun deleteOccurrencesBeforeDate(date: LocalDate) {
        val cal = getCal()

        cal.events
            .filterExceptions()
            .forEach { event ->
                if (event.dateStart.value.before(date.toStartOfDayDate())) {
                    val iterator = event.getDateIterator(TimeZone.getDefault())
                    iterator.advanceTo(date.toStartOfDayDate())
                    event.dateStart = DateStart(iterator.next(), false)
                }
            }

        cal.events
            .filterNotException()
            .forEach { exception ->
                if (exception.dateStart.value.before(date.toStartOfDayDate())) {
                    exception.status.value = Status.CANCELLED
                }
            }

        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    suspend fun updateAllOccurrencesAfterDate(
        afterDate: LocalDate,
        newRecurringExpense: RecurringExpense,
    ) {
        val cal = getCal()

        // Put an end date to all existing events (and memoize the biggest existing end date to re-apply it to the new event later)
        var newEventEndDate: Date? = null
        cal.events
            .filterExceptions()
            .filter { it.dateEnd == null || it.dateEnd.value.after(afterDate.toStartOfDayDate()) }
            .forEach {
                if (it.dateEnd !== null && (newEventEndDate == null || it.dateEnd.value.after(newEventEndDate))) {
                    newEventEndDate = it.dateEnd.value
                }

                it.dateEnd = DateEnd(afterDate.minusDays(1).toStartOfDayDate(), false)
            }

        // Remove all exceptions after the date
        cal.events
            .removeAll { event ->
                event.isException && !event.dateStart.value.before(afterDate.toStartOfDayDate())
            }

        // Create a new event starting at the date with the new properties
        val newEvent = VEvent()
        newEvent.dateStart = DateStart(newRecurringExpense.recurringDate.toStartOfDayDate(), false)
        if (newEventEndDate != null) {
            newEvent.dateEnd = DateEnd(newEventEndDate, false)
        }
        newEvent.summary = Summary(newRecurringExpense.title)
        newEvent.addExperimentalProperty(AMOUNT_KEY, newRecurringExpense.amount.getDBValue().toString())
        newEvent.addExperimentalProperty(CHECKED_KEY, false.toString())
        newEvent.status = Status.accepted()
        newEvent.recurrenceRule = RecurrenceRule(newRecurringExpense.type.toRecurrence())

        cal.addEvent(newEvent)

        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    suspend fun getFirstOccurrenceDate(): LocalDate {
        val cal = getCal()

        val firstEventTimestamp = cal.events
            .filter { !it.status.isCancelled }
            .minBy { it.dateStart.value.time }
            .dateStart.value.time

        return localDateFromTimestamp(firstEventTimestamp)
    }

    suspend fun getFirstOccurrence(): Expense {
        val cal = getCal()
        val event = cal.events.first()

        val firstOccurrenceDate = localDateFromTimestamp(event.dateStart.value.time)
        return cal.getExpenses(
            firstOccurrenceDate,
            firstOccurrenceDate.plusDays(1),
            toRecurringExpense()
        ).first()
    }

    suspend fun markAllOccurrencesAsChecked(beforeDate: LocalDate) {
        val cal = getCal()
        val expenses = cal.getExpenses(from = null, beforeDate, toRecurringExpense())
        for (expense in expenses) {
            if (expense.date == beforeDate) {
                continue
            }

            cal.addExceptionFromExpense(
                expense = expense.copy(checked = true),
                originalOccurrenceDate = expense.associatedRecurringExpense?.originalDate ?: expense.date,
            )
        }

        iCalRepresentation = withContext(Dispatchers.Default) {
            cal.write()
        }
    }

    private suspend fun getCal(): ICalendar = withContext(Dispatchers.Default) {
        Biweekly.parse(iCalRepresentation)
            .first()
            .apply {
                setProductId(null as String?)
            }
    }

    private data class EventInRange(
        val event: VEvent,
        val date: Date,
        val originalDate: Date,
    )

    private suspend fun ICalendar.getExpenses(from: LocalDate?, to: LocalDate, recurringExpense: RecurringExpense): List<Expense> = withContext(Dispatchers.Default) {
        val startDate = from?.toStartOfDayDate()
        val endDate = to.toStartOfDayDate()

        val exceptions = events
            .filterNotException()
            .associateBy { it.recurrenceId.value }

        val eventsInRange = mutableListOf<EventInRange>()

        exceptions.forEach { (date, event) ->
            if (!event.status.isCancelled && !event.dateStart.value.after(endDate) && (startDate == null || !event.dateStart.value.before(startDate))) {
                eventsInRange.add(EventInRange(event, event.dateStart.value, date))
            }
        }

        events
            .filterExceptions()
            .forEach { recurrentEvent ->
                val eventDateIterator = recurrentEvent.getDateIterator(TimeZone.getDefault())
                if (startDate != null) {
                    eventDateIterator.advanceTo(startDate)
                }

                while (eventDateIterator.hasNext()) {
                    val eventOccurrenceDate = eventDateIterator.next()
                    if (eventOccurrenceDate.after(endDate)) {
                        break
                    }

                    val dateEnd = recurrentEvent.dateEnd
                    if (dateEnd != null && eventOccurrenceDate.after(dateEnd.value)) {
                        break
                    }

                    if ((startDate == null || !eventOccurrenceDate.before(startDate)) && !eventOccurrenceDate.after(endDate)) {
                        // Check if there is an exception for this instance
                        val eventException = exceptions[eventOccurrenceDate]
                        if (eventException == null) {
                            // No exception for this instance, use the original event
                            eventsInRange.add(EventInRange(recurrentEvent, eventOccurrenceDate, eventOccurrenceDate))
                        }
                    }
                }
            }

        return@withContext eventsInRange
            .map { (event, date, originalDate) ->
                Expense(
                    id = event.uid.value.hashCode() + date.time,
                    title = event.summary.value,
                    amount = event.getExperimentalProperty(AMOUNT_KEY).value.toLong() / 100.0,
                    date = localDateFromTimestamp(date.time),
                    checked = event.getExperimentalProperty(CHECKED_KEY)?.value == "true",
                    associatedRecurringExpense = AssociatedRecurringExpense(
                        recurringExpense = recurringExpense,
                        originalDate = localDateFromTimestamp(originalDate.time),
                    ),
                )
            }
    }

    private fun List<VEvent>.filterExceptions() = filter { !it.isException }

    private fun List<VEvent>.filterNotException() = filter { it.isException }

    private val VEvent.isException get() = recurrenceId != null

    fun persistOrThrow(transaction: PowerSyncTransaction) {
        transaction.execute(
            "UPDATE $RECURRING_EXPENSE_TABLE_NAME SET i_cal_representation = ? WHERE id = ?",
            listOf(iCalRepresentation, id)
        )
    }

    companion object {
        fun fromCursorOrThrow(cursor: SqlCursor) = RecurringExpenseEntity(
            id = cursor.getLong(RECURRING_EXPENSE_ID_COLUMN_INDEX)!!,
            accountId = cursor.getString(RECURRING_EXPENSE_ACCOUNT_ID_COLUMN_INDEX)!!,
            iCalRepresentation = cursor.getString(RECURRING_EXPENSE_I_CAL_REPRESENTATION_COLUMN_INDEX)!!,
        )

        fun createFromRecurringExpenseOrThrow(
            recurringExpenseId: Long,
            recurringExpense: RecurringExpense,
            account: Account,
            transaction: PowerSyncTransaction
        ): RecurringExpenseEntity {
            val cal = ICalendar().apply { setProductId(null as String?) }
            val event = VEvent()

            event.summary = Summary(recurringExpense.title)
            event.addExperimentalProperty(AMOUNT_KEY, recurringExpense.amount.getDBValue().toString())
            event.addExperimentalProperty(CHECKED_KEY, false.toString())
            event.status = Status.accepted()
            event.dateStart = DateStart(recurringExpense.recurringDate.toStartOfDayDate(), false)
            event.recurrenceRule = RecurrenceRule(recurringExpense.type.toRecurrence())
            cal.addEvent(event)

            transaction.execute("INSERT INTO $RECURRING_EXPENSE_TABLE_NAME (id, account_id, i_cal_representation) VALUES (?, ?, ?)",
                listOf(recurringExpenseId, account.id, cal.write())
            )

            return RecurringExpenseEntity(
                id = recurringExpenseId,
                accountId = account.id,
                iCalRepresentation = cal.write(),
            )
        }
    }
}