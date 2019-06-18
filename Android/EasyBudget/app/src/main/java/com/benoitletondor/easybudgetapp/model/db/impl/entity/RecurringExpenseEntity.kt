package com.benoitletondor.easybudgetapp.model.db.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import java.util.*

@Entity(tableName = "monthlyexpense")
class RecurringExpenseEntity(@PrimaryKey
                             @ColumnInfo(name = "_expense_id")
                             val id: Long?,
                             @ColumnInfo(name = "title")
                             val title: String,
                             @ColumnInfo(name = "amount")
                             val originalAmount: Long,
                             @ColumnInfo(name = "recurringDate")
                             val recurringDate: Date,
                             @ColumnInfo(name = "modified")
                             val modified: Boolean, // Not implemented yet
                             @ColumnInfo(name = "type")
                             val type: String) {

    fun toRecurringExpense() = RecurringExpense(
        id,
        title,
        originalAmount / 100.0,
        recurringDate,
        modified,
        RecurringExpenseType.valueOf(type)
    )
}