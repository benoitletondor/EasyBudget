package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import androidx.room.PrimaryKey
import biweekly.Biweekly
import biweekly.ICalendar
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.helper.toRecurringExpenseType
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.types.RealmObject
import java.security.SecureRandom

class RecurringExpenseEntity() : RealmObject {
    @PrimaryKey
    var id: Long = SecureRandom().nextLong()
    var iCalRepresentation: String = ""

    constructor(
        representation: String,
    ) : this() {
        this.iCalRepresentation = representation
    }

    private fun getCal(): ICalendar = Biweekly.parse(iCalRepresentation).first()

    fun toRecurringExpense(): RecurringExpense {
        val event = getCal().events.first()

        val startDate = localDateFromTimestamp(event.dateStart.value.time)
        val title = event.summary.value
        val originalAmount = event.getExperimentalProperty("amount").value.toDouble()
        val recurrenceExpenseType = event.recurrenceRule.value.toRecurringExpenseType()

        return RecurringExpense(
            id,
            title,
            originalAmount / 100.0,
            startDate,
            modified = false,
            recurrenceExpenseType,
        )
    }
}