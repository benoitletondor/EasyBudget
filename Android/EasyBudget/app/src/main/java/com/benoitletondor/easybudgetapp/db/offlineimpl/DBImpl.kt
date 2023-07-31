/*
 *   Copyright 2023 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.db.offlineimpl

import androidx.sqlite.db.SimpleSQLiteQuery
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.db.restoreAction
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class DBImpl(private val roomDB: RoomDB) : DB {
    private val onChangeMutableFlow = MutableSharedFlow<Unit>()
    override val onChangeFlow: Flow<Unit> = onChangeMutableFlow

    override fun ensureDBCreated() {
        roomDB.openHelper.writableDatabase
    }

    override suspend fun triggerForceWriteToDisk() {
        roomDB.expenseDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newId = roomDB.expenseDao().persistExpense(expense.toExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return expense.copy(id = newId)
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean {
        return roomDB.expenseDao().hasExpenseForDay(dayDate) > 0
    }

    override suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean {
        return roomDB.expenseDao().hasUncheckedExpenseForDay(dayDate) > 0
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        return roomDB.expenseDao().getExpensesForDay(dayDate).toExpenses(this)
    }

    override suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense> {
        val monthEndDate = monthStartDate
            .plusMonths(1)
            .minusDays(1)

        return roomDB.expenseDao().getExpensesForMonth(monthStartDate, monthEndDate).toExpenses(this)
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        return roomDB.expenseDao().getBalanceForDay(dayDate).getRealValueFromDB()
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        return roomDB.expenseDao().getCheckedBalanceForDay(dayDate).getRealValueFromDB()
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        val newId = roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return recurringExpense.copy(id = newId)
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction {
        if( recurringExpense.id == null ) {
            throw IllegalArgumentException("deleteRecurringExpense called with a recurring expense that has no id")
        }

        val allExpenses = getAllExpenseForRecurringExpense(recurringExpense)

        deleteAllExpenseForRecurringExpense(recurringExpense)
        roomDB.expenseDao().deleteRecurringExpense(recurringExpense.toRecurringExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return restoreAction {
            persistRecurringExpense(recurringExpense)
            for(expense in allExpenses) {
                persistExpense(expense)
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    override suspend fun deleteExpense(expense: Expense): RestoreAction {
        if( expense.id == null ) {
            throw IllegalArgumentException("deleteExpense called with an expense that has no id")
        }

        roomDB.expenseDao().deleteExpense(expense.toExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return restoreAction {
            persistExpense(expense)
            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpense called with a recurring expense that has no id")

        roomDB.expenseDao().deleteAllExpenseForRecurringExpense(recurringExpenseId)
    }

    private suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpenseForRecurringExpense called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpenseForRecurringExpense(recurringExpenseId).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): RestoreAction {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseFromDate called with a recurring expense that has no id")

        val allExpenses = getAllExpensesForRecurringExpenseAfterDate(recurringExpense, afterDate)

        roomDB.expenseDao().deleteAllExpenseForRecurringExpenseAfterDate(recurringExpenseId, afterDate)

        onChangeMutableFlow.emit(Unit)

        return restoreAction {
            for(expense in allExpenses) {
                persistExpense(expense)
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun getAllExpensesForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseAfterDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseAfterDate(recurringExpenseId, afterDate).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): RestoreAction {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        val allExpenses = getAllExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

        roomDB.expenseDao().deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate)

        onChangeMutableFlow.emit(Unit)

        return restoreAction {
            for(expense in allExpenses) {
                persistExpense(expense)
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate).toExpenses(this)
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): Boolean {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("hasExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().hasExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate) > 0
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        return roomDB.expenseDao().findRecurringExpenseForId(recurringExpenseId)?.toRecurringExpense()
    }

    override suspend fun getOldestExpense(): Expense? {
        return roomDB.expenseDao().getOldestExpense()?.toExpense(this)
    }

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate) {
        roomDB.expenseDao().markAllEntriesAsChecked(beforeDate)

        onChangeMutableFlow.emit(Unit)
    }

    override fun close() {
        roomDB.close()
    }
}

private suspend fun List<ExpenseEntity>.toExpenses(db: DB): List<Expense> {
    return map { it.toExpense(db) }
}

private suspend fun ExpenseEntity.toExpense(db: DB): Expense {
    val recurringExpense = this.associatedRecurringExpenseId?.let { id -> db.findRecurringExpenseForId(id) }
    return toExpense(recurringExpense)
}

private fun Expense.toExpenseEntity() = ExpenseEntity(
    id,
    title,
    amount.getDBValue(),
    date,
    checked,
    associatedRecurringExpense?.recurringExpense?.id,
)

private fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity (
    id,
    title,
    amount.getDBValue(),
    recurringDate,
    modified,
    type.name
)