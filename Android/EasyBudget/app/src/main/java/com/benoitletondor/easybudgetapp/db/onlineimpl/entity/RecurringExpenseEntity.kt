package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.DateEnd
import biweekly.property.DateStart
import biweekly.property.ProductId
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
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var accountId: String = ""
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

    suspend fun toRecurringExpense(): RecurringExpense {
        return withContext(Dispatchers.IO) {
            val event = getCal().events.first()

            val startDate = localDateFromTimestamp(event.dateStart.value.time)
            val title = event.summary.value
            val originalAmount = event.getExperimentalProperty(AMOUNT_KEY).value.toLong()
            val recurrenceExpenseType = event.recurrenceRule.value.toRecurringExpenseType()

            return@withContext RecurringExpense(
                _id,
                title,
                originalAmount.getRealValueFromDB(),
                startDate,
                modified = false,
                recurrenceExpenseType,
            )
        }
    }

    suspend fun generateExpenses(from: LocalDate, to: LocalDate): List<Expense> {
        return withContext(Dispatchers.IO) {
            getCal().getExpenses(from, to, toRecurringExpense())
        }
    }

    suspend fun addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        withContext(Dispatchers.IO) {
            val cal = getCal()
            cal.addExceptionFromExpense(expense, originalOccurrenceDate)
            iCalRepresentation = cal.write()
        }
    }

    private fun ICalendar.addExceptionFromExpense(expense: Expense, originalOccurrenceDate: LocalDate) {
        val exceptionEvent = VEvent()
        exceptionEvent.dateStart = DateStart(expense.date.toStartOfDayDate(), false)
        exceptionEvent.summary = Summary(expense.title)
        exceptionEvent.status = Status.accepted()
        exceptionEvent.addExperimentalProperty(AMOUNT_KEY, expense.amount.getDBValue().toString())
        exceptionEvent.addExperimentalProperty(CHECKED_KEY, expense.checked.toString())
        exceptionEvent.uid = events.first().uid

        val recurrenceId = RecurrenceId(ICalDate(originalOccurrenceDate.toStartOfDayDate(), false))
        exceptionEvent.recurrenceId = recurrenceId

        addEvent(exceptionEvent)
    }

    suspend fun deleteOccurrence(occurrenceDate: LocalDate) {
        withContext(Dispatchers.IO) {
            val cal = getCal()

            val exceptionEvent = VEvent()
            exceptionEvent.dateStart = DateStart(occurrenceDate.toStartOfDayDate(), false)
            exceptionEvent.uid = cal.events.first().uid
            exceptionEvent.status = Status.cancelled()
            val recurrenceId = RecurrenceId(ICalDate(occurrenceDate.toStartOfDayDate(), false))
            exceptionEvent.recurrenceId = recurrenceId

            cal.addEvent(exceptionEvent)

            iCalRepresentation = cal.write()
        }
    }

    suspend fun deleteOccurrencesAfterDate(date: LocalDate) {
        withContext(Dispatchers.IO) {
            val cal = getCal()

            val event = cal.events.first()
            event.dateEnd = DateEnd(date.toStartOfDayDate(), false)

            iCalRepresentation = cal.write()
        }
    }

    suspend fun deleteOccurrencesBeforeDate(date: LocalDate) {
        withContext(Dispatchers.IO) {
            val cal = getCal()

            val event = cal.events.first()
            event.dateStart = DateStart(date.toStartOfDayDate(), false)

            iCalRepresentation = cal.write()
        }
    }

    suspend fun getFirstOccurrenceDate(): LocalDate {
        return withContext(Dispatchers.IO) {
            val cal = getCal()

            val event = cal.events.first()

            return@withContext localDateFromTimestamp(event.dateStart.value.time)
        }
    }

    suspend fun getFirstOccurrence(): Expense {
        return withContext(Dispatchers.IO) {
            val cal = getCal()
            val event = cal.events.first()

            val firstOccurrenceDate = localDateFromTimestamp(event.dateStart.value.time)
            return@withContext cal.getExpenses(
                firstOccurrenceDate,
                firstOccurrenceDate.plusDays(1),
                toRecurringExpense()
            ).first()
        }
    }

    suspend fun markAllOccurrencesAsChecked(beforeDate: LocalDate) {
        return withContext(Dispatchers.IO) {
            val cal = getCal()
            val expenses = cal.getExpenses(LocalDate.MIN, beforeDate, toRecurringExpense())
            for (expense in expenses) {
                if (expense.date == beforeDate) {
                    continue
                }

                cal.addExceptionFromExpense(expense.copy(checked = true), expense.date)
            }

            iCalRepresentation = cal.write()
        }
    }

    private suspend fun getCal(): ICalendar = Biweekly.parse(iCalRepresentation)
        .first()
        .apply {
            setProductId(null as String?)
        }

    private suspend fun ICalendar.getExpenses(from: LocalDate, to: LocalDate, recurringExpense: RecurringExpense): List<Expense> {
        val startDate = from.toStartOfDayDate()
        val endDate = to.toStartOfDayDate()

        val exceptions: MutableMap<Date, VEvent> = mutableMapOf()
        val recurrentEvent = events.first()

        for (event in events) {
            val recurrenceId = event.recurrenceId
            if (recurrenceId != null) {
                // This is an exception event, store it in the map.
                exceptions[recurrenceId.value] = event
            }
        }

        val eventsInRange = mutableListOf<Pair<VEvent, Date>>()
        val eventDateIterator = recurrentEvent.getDateIterator(TimeZone.getDefault())
        eventDateIterator.advanceTo(startDate)

        while (eventDateIterator.hasNext()) {
            val eventOccurrenceDate = eventDateIterator.next()
            if (eventOccurrenceDate.after(endDate)) {
                break
            }

            if (!eventOccurrenceDate.before(startDate) && !eventOccurrenceDate.after(endDate)) {
                // Check if there is an exception for this instance
                val eventException = exceptions[eventOccurrenceDate]
                if (eventException != null) {
                    if (!eventException.status.isCancelled) {
                        // This instance has been replaced by an exception and isn't cancelled
                        eventsInRange.add(Pair(eventException, eventOccurrenceDate))
                    }
                } else {
                    // No exception for this instance, use the original event
                    eventsInRange.add(Pair(recurrentEvent, eventOccurrenceDate))
                }
            }
        }

        return eventsInRange
            .map { (event, date) ->
                Expense(
                    id = event.uid.value.hashCode() + date.time,
                    title = event.summary.value,
                    amount = event.getExperimentalProperty(AMOUNT_KEY).value.toLong() / 100.0,
                    date = localDateFromTimestamp(date.time),
                    checked = event.getExperimentalProperty(CHECKED_KEY)?.value == "true",
                    associatedRecurringExpense = AssociatedRecurringExpense(
                        recurringExpense = recurringExpense,
                        originalDate = localDateFromTimestamp(event.dateStart.value.time)
                    ),
                )
            }
    }

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