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

package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

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
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.helper.toRecurrence
import com.benoitletondor.easybudgetapp.helper.toRecurringExpenseType
import com.benoitletondor.easybudgetapp.helper.toStartOfDayDate
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone

private const val AMOUNT_KEY = "amount"
private const val CHECKED_KEY = "checked"

class RecurringExpenseEntity() : RealmObject {
    @PrimaryKey
    var _id: Long = SecureRandom().nextLong()
    var iCalRepresentation: String = ""
    @Index
    var accountId: String = ""
    @Index
    var accountSecret: String = ""

    constructor(
        id: Long?,
        representation: String,
        account: Account,
    ) : this() {
        this._id = id ?: SecureRandom().nextLong()
        this.iCalRepresentation = representation
        this.accountId = account.id
        this.accountSecret = account.secret
    }

    fun toRecurringExpense(): RecurringExpense {
        val event = getCal().events
            .filterExceptions()
            .last()

        val startDate = localDateFromTimestamp(event.dateStart.value.time)
        val title = event.summary.value
        val originalAmount = event.getExperimentalProperty(AMOUNT_KEY).value.toLong()
        val recurrenceExpenseType = event.recurrenceRule.value.toRecurringExpenseType()

        return RecurringExpense(
            _id,
            title,
            originalAmount.getRealValueFromDB(),
            startDate,
            modified = false,
            recurrenceExpenseType,
        )
    }

    fun generateExpenses(from: LocalDate?, to: LocalDate): List<Expense> {
        return getCal().getExpenses(from, to, toRecurringExpense())
    }

    fun addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        val cal = getCal()
        cal.addExceptionFromExpense(expense, originalOccurrenceDate)
        iCalRepresentation = cal.write()
    }

    private fun ICalendar.addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(expense.date.toStartOfDayDate(), false)
        exceptionEvent.summary = Summary(expense.title)
        exceptionEvent.status = Status.accepted()
        exceptionEvent.addExperimentalProperty(AMOUNT_KEY, expense.amount.getDBValue().toString())
        exceptionEvent.addExperimentalProperty(CHECKED_KEY, expense.checked.toString())
        exceptionEvent.uid = events
            .filterExceptions()
            .first {
                it.dateEnd == null || !it.dateEnd.value.before(originalOccurrenceDate.toStartOfDayDate())
            }
            .uid

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

    fun deleteOccurrence(occurrenceDate: LocalDate, originalOccurrenceDate: LocalDate) {
        val cal = getCal()

        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(occurrenceDate.toStartOfDayDate(), false)
        exceptionEvent.uid = cal.events
            .filterExceptions()
            .first { it.dateEnd == null || !it.dateEnd.value.before(originalOccurrenceDate.toStartOfDayDate()) }
            .uid
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

        iCalRepresentation = cal.write()
    }

    fun deleteOccurrencesAfterDate(date: LocalDate) {
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

        iCalRepresentation = cal.write()
    }

    fun deleteOccurrencesBeforeDate(date: LocalDate) {
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

        iCalRepresentation = cal.write()
    }

    fun updateAllOccurrencesAfterDate(
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
        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(newRecurringExpense.recurringDate.toStartOfDayDate(), false)
        if (newEventEndDate != null) {
            exceptionEvent.dateEnd = DateEnd(newEventEndDate, false)
        }
        exceptionEvent.summary = Summary(newRecurringExpense.title)
        exceptionEvent.addExperimentalProperty(AMOUNT_KEY, newRecurringExpense.amount.getDBValue().toString())
        exceptionEvent.addExperimentalProperty(CHECKED_KEY, false.toString())
        exceptionEvent.status = Status.accepted()
        exceptionEvent.recurrenceRule = RecurrenceRule(newRecurringExpense.type.toRecurrence())

        cal.addEvent(exceptionEvent)

        iCalRepresentation = cal.write()
    }

    fun getFirstOccurrenceDate(): LocalDate {
        val cal = getCal()

        val firstEventTimestamp = cal.events
            .filter { !it.status.isCancelled }
            .minBy { it.dateStart.value.time }
            .dateStart.value.time

        return localDateFromTimestamp(firstEventTimestamp)
    }

    fun getFirstOccurrence(): Expense {
        val cal = getCal()
        val event = cal.events.first()

        val firstOccurrenceDate = localDateFromTimestamp(event.dateStart.value.time)
        return cal.getExpenses(
            firstOccurrenceDate,
            firstOccurrenceDate.plusDays(1),
            toRecurringExpense()
        ).first()
    }

    fun markAllOccurrencesAsChecked(beforeDate: LocalDate) {
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

        iCalRepresentation = cal.write()
    }

    private fun getCal(): ICalendar = Biweekly.parse(iCalRepresentation)
        .first()
        .apply {
            setProductId(null as String?)
        }

    private data class EventInRange(
        val event: VEvent,
        val date: Date,
        val originalDate: Date,
    )

    private fun ICalendar.getExpenses(from: LocalDate?, to: LocalDate, recurringExpense: RecurringExpense): List<Expense> {
        val startDate = from?.toStartOfDayDate()
        val endDate = to.toStartOfDayDate()

        val exceptions: MutableMap<Date, VEvent> = mutableMapOf()

        for (event in events) {
            val recurrenceId = event.recurrenceId
            if (recurrenceId != null) {
                // This is an exception event, store it in the map.
                exceptions[recurrenceId.value] = event
            }
        }

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

        return eventsInRange
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

    companion object {
        fun newFromRecurringExpense(recurringExpense: RecurringExpense, account: Account): RecurringExpenseEntity {
            val cal = ICalendar().apply { setProductId(null as String?) }
            val event = VEvent()

            event.summary = Summary(recurringExpense.title)
            event.addExperimentalProperty(AMOUNT_KEY, recurringExpense.amount.getDBValue().toString())
            event.addExperimentalProperty(CHECKED_KEY, false.toString())
            event.status = Status.accepted()
            event.dateStart = DateStart(recurringExpense.recurringDate.toStartOfDayDate(), false)
            event.recurrenceRule = RecurrenceRule(recurringExpense.type.toRecurrence())
            cal.addEvent(event)

            return RecurringExpenseEntity(
                id = recurringExpense.id,
                representation = cal.write(),
                account = account,
            )
        }
    }
}