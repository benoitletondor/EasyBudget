package com.benoitletondor.easybudgetapp

import android.R.attr.end
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.DateEnd
import biweekly.property.RecurrenceId
import biweekly.property.Summary
import biweekly.util.Frequency
import biweekly.util.Recurrence
import biweekly.util.com.google.ical.compat.javautil.DateIterator
import biweekly.util.com.google.ical.iter.RecurrenceIteratorFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone


class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val ical = ICalendar()
            val event = VEvent()
            event.summary = Summary("Coucou")
            event.setDateStart(Date(), false)
            val recur = Recurrence.Builder(Frequency.WEEKLY).interval(2).build()
            event.setRecurrenceRule(recur)

            val exceptionEvent = VEvent()
            exceptionEvent.dateStart = event.dateStart
            exceptionEvent.summary = Summary("Exception")
            exceptionEvent.uid = event.uid

            val recurrenceId = RecurrenceId(event.dateStart.value)
            exceptionEvent.recurrenceId = recurrenceId

            ical.addEvent(event)
            ical.addEvent(exceptionEvent)
            println(Biweekly.write(ical).go())


            val startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
            val endDate = Date.from(LocalDate.now().plusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant())

            val exceptions: MutableMap<String, MutableMap<Date, VEvent>> = mutableMapOf()
            val recurrentEvents: MutableMap<String, VEvent> = mutableMapOf()

            for (event in ical.events) {
                val recurrenceId = event.recurrenceId
                if (recurrenceId != null) {
                    // This is an exception event, store it in the map.
                    exceptions.computeIfAbsent(event.uid.value) { HashMap() }[recurrenceId.value] = event
                } else if (event.recurrenceRule != null) {
                    // This is a recurrent event
                    recurrentEvents[event.uid.value] = event
                }
            }

            val eventsInRange: MutableList<VEvent?> = ArrayList()
            for (event in recurrentEvents.values) {
                val it: DateIterator = event.getDateIterator(TimeZone.getDefault())
                while (it.hasNext()) {
                    val nextInstance = it.next()
                    if (nextInstance.after(endDate)) {
                        break
                    }

                    if (nextInstance.equals(startDate) || nextInstance.equals(endDate) || (nextInstance.after(startDate) && nextInstance.before(endDate))) {
                        // Check if there is an exception for this instance
                        val eventExceptions: Map<Date, VEvent>? = exceptions[event.uid.value]
                        if (eventExceptions != null && eventExceptions.containsKey(nextInstance)) {
                            // This instance has been replaced by an exception
                            eventsInRange.add(eventExceptions[nextInstance])
                        } else {
                            // No exception for this instance, use the original event
                            eventsInRange.add(event)
                        }
                    }
                }
            }

            for (event in eventsInRange) {
                val summary = event!!.summary
                if (summary != null) {
                    println(event.summary.value)
                }
            }
        }
    }
}