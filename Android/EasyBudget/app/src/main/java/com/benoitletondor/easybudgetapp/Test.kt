package com.benoitletondor.easybudgetapp

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.RecurrenceId
import biweekly.property.Summary
import biweekly.util.Frequency
import biweekly.util.Recurrence
import com.benoitletondor.easybudgetapp.helper.toStartOfDayDate
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone


class Main {
    data class Expense(val title: String, val amount: Double)

    companion object {
        private fun ICalendar.getExpense(from: LocalDate, to: LocalDate): List<Expense> {
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

            val eventsInRange = mutableListOf<VEvent>()
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
                            eventsInRange.add(eventException)
                        } else {
                            // No exception for this instance, use the original event
                            eventsInRange.add(event)
                        }
                    }
                }
            }

            return eventsInRange
                .map {
                    Expense(
                        it.summary.value,
                        it.getExperimentalProperty("amount").value.toDouble() / 100.0
                    )
                }
        }

        @JvmStatic fun main(args: Array<String>) {
            val ical = ICalendar()
            val event = VEvent()
            event.addExperimentalProperty("amount", 20020.0.toString())
            event.summary = Summary("Coucou")
            event.setDateStart(Date(), false)
            val recur = Recurrence.Builder(Frequency.WEEKLY).interval(2).build()
            event.setRecurrenceRule(recur)

            val exceptionEvent = VEvent()
            exceptionEvent.dateStart = event.dateStart
            exceptionEvent.summary = Summary("Exception")
            exceptionEvent.addExperimentalProperty("amount", 21022.0.toString())
            exceptionEvent.uid = event.uid

            val recurrenceId = RecurrenceId(event.dateStart.value)
            exceptionEvent.recurrenceId = recurrenceId

            ical.addEvent(event)
            ical.addEvent(exceptionEvent)
            println(Biweekly.write(ical).go())

            println(event.recurrenceRule.value.frequency)
            println(event.recurrenceRule.value.interval)

            val expenses = ical.getExpense(LocalDate.now(), LocalDate.now().plusDays(60))
            for (expense in expenses) {
                println("title: " + expense.title + " / amount: "+ expense.amount)
            }
        }
    }
}