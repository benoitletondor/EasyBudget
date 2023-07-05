package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import com.benoitletondor.easybudgetapp.model.Expense
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import java.security.SecureRandom
import java.time.LocalDate

class ExpenseEntity() : RealmObject {
    @PrimaryKey
    var id: Long = SecureRandom().nextLong()
    var title: String = ""
    var amount: Long = 0L
    @Index
    var date: Long = 0L
    var checked: Boolean = false

    constructor(
        title: String,
        amount: Long,
        date: Long,
        checked: Boolean,
    ) : this() {
        this.title = title
        this.amount = amount
        this.date = date
        this.checked = checked
    }

    fun toExpense() = Expense(
        id,
        title,
        amount / 100.0,
        LocalDate.ofEpochDay(date),
        checked,
        associatedRecurringExpense = null,
    )
}