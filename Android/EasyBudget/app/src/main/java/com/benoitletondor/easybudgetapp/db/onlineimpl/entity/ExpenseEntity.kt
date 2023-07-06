package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import com.benoitletondor.easybudgetapp.model.Expense
import java.time.LocalDate

data class ExpenseEntity(
    val id: Long,
    val title: String,
    val amount: Long,
    val date: Long,
    val checked: Boolean,
) {
    fun toExpense() = Expense(
        id,
        title,
        amount / 100.0,
        LocalDate.ofEpochDay(date),
        checked,
        associatedRecurringExpense = null,
    )
}