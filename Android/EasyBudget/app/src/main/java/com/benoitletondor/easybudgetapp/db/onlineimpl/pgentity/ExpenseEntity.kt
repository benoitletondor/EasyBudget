/*
 *   Copyright 2025 Benoit Letondor
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity

import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.powersync.db.SqlCursor
import com.powersync.db.internal.PowerSyncTransaction
import com.powersync.db.schema.Column
import com.powersync.db.schema.Index
import com.powersync.db.schema.IndexedColumn
import com.powersync.db.schema.Table
import java.time.LocalDate

private const val EXPENSE_TABLE_NAME = "expense"
private const val EXPENSE_ID_COLUMN_INDEX = 0
private const val EXPENSE_ACCOUNT_ID_COLUMN_INDEX = 1
private const val EXPENSE_TITLE_COLUMN_INDEX = 2
private const val EXPENSE_AMOUNT_COLUMN_INDEX = 3
private const val EXPENSE_DATE_COLUMN_INDEX = 4
private const val EXPENSE_CHECKED_COLUMN_INDEX = 5

val expenseEntityTable = Table(
    EXPENSE_TABLE_NAME,
    listOf(
        Column.text("account_id"),
        Column.text("title"),
        Column.integer("amount"),
        Column.integer("date"),
        Column.integer("checked"),
        Column.text("created_at"),
    ),
    indexes = listOf(
        Index(
            name = "date_index",
            columns = listOf(IndexedColumn.ascending("date"))
        ),
        Index(
            name = "date_created_at_index",
            columns = listOf(IndexedColumn.ascending("date"), IndexedColumn.ascending("created_at")),
        ),
        Index(
            name = "date_checked_index",
            columns = listOf(IndexedColumn.ascending("date"), IndexedColumn("checked"))
        ),
    ),
)

class ExpenseEntity(
    val id: Long,
    val accountId: String,
    private var title: String,
    private var amount: Long,
    private var date: Long,
    private var checked: Boolean,
) {
    fun toExpense(associatedRecurringExpense: RecurringExpense?) = Expense(
        id,
        title,
        amount.getRealValueFromDB(),
        LocalDate.ofEpochDay(date),
        checked,
        associatedRecurringExpense?.let { AssociatedRecurringExpense(
            recurringExpense = it,
            originalDate = it.recurringDate,
        ) },
    )

    fun persistOrThrow(transaction: PowerSyncTransaction) {
        transaction.execute(
            "UPDATE $EXPENSE_TABLE_NAME SET title = ?, amount = ?, date = ?, checked = ? WHERE id = ?",
            listOf(title, amount, date, checked, id)
        )
    }

    companion object {
        fun fromCursorOrThrow(cursor: SqlCursor) = ExpenseEntity(
            id = cursor.getLong(EXPENSE_ID_COLUMN_INDEX)!!,
            accountId = cursor.getString(EXPENSE_ACCOUNT_ID_COLUMN_INDEX)!!,
            title = cursor.getString(EXPENSE_TITLE_COLUMN_INDEX)!!,
            amount = cursor.getLong(EXPENSE_AMOUNT_COLUMN_INDEX)!!,
            date = cursor.getLong(EXPENSE_DATE_COLUMN_INDEX)!!,
            checked = cursor.getBoolean(EXPENSE_CHECKED_COLUMN_INDEX)!!,
        )

        fun createFromExpenseOrThrow(expenseId: Long, expense: Expense, account: Account, transaction: PowerSyncTransaction): ExpenseEntity {
            transaction.execute(
                "INSERT INTO $EXPENSE_TABLE_NAME (id, account_id, title, amount, date, checked) VALUES (?, ?, ?, ?, ?, ?)",
                listOf(expenseId, account.id, expense.title, expense.amount.getDBValue(), expense.date.toEpochDay(), expense.checked)
            )

            return fromExpense(expenseId, expense, account)
        }

        fun fromExpense(expenseId: Long, expense: Expense, account: Account): ExpenseEntity {
            return ExpenseEntity(
                id = expenseId,
                accountId = account.id,
                title = expense.title,
                amount = expense.amount.getDBValue(),
                date = expense.date.toEpochDay(),
                checked = expense.checked,
            )
        }
    }
}