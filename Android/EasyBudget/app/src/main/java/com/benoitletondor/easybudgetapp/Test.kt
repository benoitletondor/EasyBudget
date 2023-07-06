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