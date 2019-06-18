package com.benoitletondor.easybudgetapp.model.db.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import java.util.*

@Entity(tableName = "expense")
data class ExpenseEntity(@PrimaryKey(autoGenerate = true)
                         @ColumnInfo(name = "_expense_id")
                         val id: Long?,
                         @ColumnInfo(name = "title")
                         val title: String,
                         @ColumnInfo(name = "amount")
                         val amount: Double,
                         @ColumnInfo(name = "date")
                         val date: Date,
                         @ColumnInfo(name = "monthly_id")
                         val associatedRecurringExpenseId: Long?) {

    fun toExpense(associatedRecurringExpense: RecurringExpense?) = Expense(
        id,
        title,
        amount,
        date,
        associatedRecurringExpense
    )
}