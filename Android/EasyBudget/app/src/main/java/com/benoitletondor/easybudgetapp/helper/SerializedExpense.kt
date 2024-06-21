package com.benoitletondor.easybudgetapp.helper

import android.os.Parcelable
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Parcelize
data class SerializedExpense(
    val id: Long?,
    val title: String,
    val amount: Double,
    val date: Long,
    val checked: Boolean,
    val associatedRecurringExpense: SerializedAssociatedRecurringExpense?
) : Parcelable {
    constructor(expense: Expense) : this(
        expense.id,
        expense.title,
        expense.amount,
        expense.date.toEpochDay(),
        expense.checked,
        expense.associatedRecurringExpense?.let { SerializedAssociatedRecurringExpense(it) },
    )

    fun toExpense(): Expense = Expense(
        id,
        title,
        amount,
        LocalDate.ofEpochDay(date),
        checked,
        associatedRecurringExpense?.toAssociatedRecurringExpense(),
    )
}

@Serializable
@Parcelize
data class SerializedAssociatedRecurringExpense(
    val recurringExpense: SerializedRecurringExpense,
    val originalDate: Long,
) : Parcelable {
    constructor(associatedRecurringExpense: AssociatedRecurringExpense) : this(
        SerializedRecurringExpense(associatedRecurringExpense.recurringExpense),
        associatedRecurringExpense.originalDate.toEpochDay(),
    )

    fun toAssociatedRecurringExpense() = AssociatedRecurringExpense(
        recurringExpense.toRecurringExpense(),
        LocalDate.ofEpochDay(originalDate),
    )
}

@Serializable
@Parcelize
data class SerializedRecurringExpense(
    val id: Long?,
    val title: String,
    val amount: Double,
    val recurringDate: Long,
    val modified: Boolean,
    val type: RecurringExpenseType
) : Parcelable {
    constructor(recurringExpense: RecurringExpense) : this(
        recurringExpense.id,
        recurringExpense.title,
        recurringExpense.amount,
        recurringExpense.recurringDate.toEpochDay(),
        recurringExpense.modified,
        recurringExpense.type,
    )

    fun toRecurringExpense() = RecurringExpense(
        id,
        title,
        amount,
        LocalDate.ofEpochDay(recurringDate),
        modified,
        type,
    )
}