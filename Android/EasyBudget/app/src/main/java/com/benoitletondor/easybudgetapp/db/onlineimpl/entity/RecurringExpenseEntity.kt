package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import androidx.room.PrimaryKey
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.helper.toRecurringExpenseType
import com.benoitletondor.easybudgetapp.helper.toStartOfDayDate
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.types.RealmObject
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone

private const val AMOUNT_KEY = "amount"
private const val CHECKED_KEY = "checked"

class RecurringExpenseEntity() : RealmObject {
    @PrimaryKey
    var id: Long = SecureRandom().nextLong()
    var iCalRepresentation: String = ""
    var account: Account? = null

    constructor(
        representation: String,
        account: Account,
    ) : this() {
        this.iCalRepresentation = representation
        this.account = account
    }

    private fun getCal(): ICalendar = Biweekly.parse(iCalRepresentation).first()

    fun toRecurringExpense(): RecurringExpense {
        val event = getCal().events.first()

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

    fun generateExpenses(from: LocalDate, to: LocalDate): List<Expense> {
        return getCal().getExpenses(from, to, toRecurringExpense())
    }

    private fun ICalendar.getExpenses(from: LocalDate, to: LocalDate, recurringExpense: RecurringExpense): List<Expense> {
        val startDate = from.toStartOfDayDate()
        val endDate = to.toStartOfDayDate()

        val exceptions: MutableMap<String, MutableMap<Date, VEvent>> = mutableMapOf()
        val recurrentEvents: MutableMap<String, VEvent> = mutableMapOf()

        for (event in events) {
            val recurrenceId = event.recurrenceId
            if (recurrenceId != null) {
                // This is an exception event, store it in the map.
                exceptions.computeIfAbsent(event.uid.value) { HashMap() }[recurrenceId.value] = event
            } else if (event.recurrenceRule != null) {
                // This is a recurrent event
                recurrentEvents[event.uid.value] = event
            }
        }

        val eventsInRange = mutableListOf<Pair<VEvent, Date>>()
        for (event in recurrentEvents.values) {
            val eventDateIterator = event.getDateIterator(TimeZone.getDefault())
            eventDateIterator.advanceTo(startDate)

            while (eventDateIterator.hasNext()) {
                val eventOccurrenceDate = eventDateIterator.next()
                if (eventOccurrenceDate.after(endDate)) {
                    break
                }

                if (!eventOccurrenceDate.before(startDate) && !eventOccurrenceDate.after(endDate)) {
                    // Check if there is an exception for this instance
                    val eventException = exceptions[event.uid.value]?.get(eventOccurrenceDate)
                    if (eventException != null) {
                        // This instance has been replaced by an exception
                        eventsInRange.add(Pair(eventException, eventOccurrenceDate))
                    } else {
                        // No exception for this instance, use the original event
                        eventsInRange.add(Pair(event, eventOccurrenceDate))
                    }
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
                    associatedRecurringExpense = recurringExpense,
                )
            }
    }
}