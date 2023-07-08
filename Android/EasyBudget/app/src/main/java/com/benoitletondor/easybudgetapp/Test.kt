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
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val ical = ICalendar()
            val event = VEvent()
            event.addExperimentalProperty("amount", 20020.0.toString())
            event.addExperimentalProperty("checked", false.toString())
            event.summary = Summary("Coucou")
            event.setDateStart(Date(), false)
            val recur = Recurrence.Builder(Frequency.WEEKLY).interval(2).build()
            event.setRecurrenceRule(recur)
            ical.addEvent(event)

            for (i in 0..50000) {
                val exceptionEvent = VEvent()
                exceptionEvent.dateStart = event.dateStart
                exceptionEvent.summary = Summary("Exception: $i")
                exceptionEvent.addExperimentalProperty("amount", 21022.0.toString())
                exceptionEvent.addExperimentalProperty("checked", true.toString())
                exceptionEvent.uid = event.uid

                val recurrenceId = RecurrenceId(event.dateStart.value)
                exceptionEvent.recurrenceId = recurrenceId

                ical.addEvent(exceptionEvent)
            }

            val now = System.currentTimeMillis()
            val json = Biweekly.write(ical).go()
            val after = System.currentTimeMillis()
            println("Time to write: ${after-now}ms")
            println("Size: ${json.toByteArray().size / 1000.0} kb")

            val now2 = System.currentTimeMillis()
            Biweekly.parse(json).all().map { it.events }
            val after2 = System.currentTimeMillis()
            println("Time to read: ${after2-now2}ms")
        }
    }
}