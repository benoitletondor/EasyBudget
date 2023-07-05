package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import androidx.room.PrimaryKey
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import io.realm.kotlin.types.RealmObject
import java.security.SecureRandom

class RecurringExpenseEntity() : RealmObject {
    @PrimaryKey
    var id: Long = SecureRandom().nextLong()
    var title: String = ""
    var iCalRepresentation: String = ""
    var type: String = ""

    constructor(
        title: String,
        representation: String,
        type: String,
    ) : this() {
        this.title = title
        this.iCalRepresentation = representation
        this.type = type
    }

    private fun getCal(): ICalendar = Biweekly.parse(iCalRepresentation).first()
    private fun getFirstEvent(): VEvent = getCal().events.first()

    fun toRecurringExpense(): RecurringExpense {
        val event = getFirstEvent()
        val startDate = localDateFromTimestamp(event.dateStart.value.time)
        val originalAmount = event.getExperimentalProperty("amount").value.toDouble()

        return RecurringExpense(
            id,
            title,
            originalAmount / 100.0,
            startDate,
            modified = false,
            RecurringExpenseType.valueOf(type),
        )
    }
}