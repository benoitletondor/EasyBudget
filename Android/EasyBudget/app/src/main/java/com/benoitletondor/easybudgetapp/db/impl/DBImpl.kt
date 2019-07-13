/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.db.impl

import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.impl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.helper.Logger
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

class DBImpl(private val roomDB: RoomDB) : DB {
    override fun ensureDBCreated() {
        roomDB.openHelper.writableDatabase.close()
    }

    override fun close() {
        roomDB.close()
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newId = roomDB.expenseDao().persistExpense(expense.toExpenseEntity())
        return expense.copy(id = newId)
    }

    override suspend fun hasExpenseForDay(dayDate: Date): Boolean {
        val (startDate, endDate) = dayDate.getDayDatesRange()
        return roomDB.expenseDao().hasExpenseForDay(startDate, endDate) > 0
    }

    override suspend fun getExpensesForDay(dayDate: Date): List<Expense> {
        val (startDate, endDate) = dayDate.getDayDatesRange()
        return roomDB.expenseDao().getExpensesForDay(startDate, endDate).toExpenses(this)
    }

    override suspend fun getExpensesForMonth(monthStartDate: Date): List<Expense> {
        val (firstDayStartDate) = monthStartDate.getDayDatesRange()

        val cal = Calendar.getInstance()
        cal.time = monthStartDate
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)

        val (_, lastDayEndDate) = cal.time.getDayDatesRange()

        return roomDB.expenseDao().getExpensesForMonth(firstDayStartDate, lastDayEndDate).toExpenses(this)
    }

    override suspend fun getBalanceForDay(dayDate: Date): Double {
        val (_, endDate) = dayDate.getDayDatesRange()

        return roomDB.expenseDao().getBalanceForDay(endDate).getRealValueFromDB()
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        val newId = roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())
        return recurringExpense.copy(id = newId)
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        if( recurringExpense.id == null ) {
            throw IllegalArgumentException("deleteRecurringExpense called with a recurring expense that has no id")
        }

        roomDB.expenseDao().deleteRecurringExpense(recurringExpense.toRecurringExpenseEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        if( expense.id == null ) {
            throw IllegalArgumentException("deleteExpense called with an expense that has no id")
        }

        roomDB.expenseDao().deleteExpense(expense.toExpenseEntity())
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpense called with a recurring expense that has no id")

        roomDB.expenseDao().deleteAllExpenseForRecurringExpense(recurringExpenseId)
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpenseForRecurringExpense called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpenseForRecurringExpense(recurringExpenseId).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date) {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseFromDate called with a recurring expense that has no id")

        return roomDB.expenseDao().deleteAllExpenseForRecurringExpenseFromDate(recurringExpenseId, fromDate)
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseFromDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseFromDate(recurringExpenseId, fromDate).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date) {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate)
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate).toExpenses(this)
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): Boolean {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("hasExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().hasExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate) > 0
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        return roomDB.expenseDao().findRecurringExpenseForId(recurringExpenseId)?.toRecurringExpense()
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
    associatedRecurringExpense?.id
)

private fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity (
    id,
    title,
    originalAmount.getDBValue(),
    recurringDate,
    modified,
    type.name
)

private data class DayDateRange(val dayStartDate: Date, val dayEndDate: Date)

private fun Date.getDayDatesRange(): DayDateRange {
    val cal = Calendar.getInstance()
    cal.time = this

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    val start = cal.time
    cal.add(Calendar.HOUR_OF_DAY, 23)
    cal.add(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val end = cal.time

    return DayDateRange(start, end)
}

/**
 * Return the integer value of the double * 100 to store it as integer in DB. This is an ugly
 * method that shouldn't be there but rounding on doubles are a pain :/
 *
 * @return the corresponding int value (double * 100)
 */
private fun Double.getDBValue(): Long {
    val stringValue = CurrencyHelper.getFormattedAmountValue(this)
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble: $stringValue")
    }

    val ceiledValue = ceil(this * 100).toLong()
    val ceiledDoubleValue = ceiledValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(ceiledDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return ceiled value: $ceiledValue")
        }
        return ceiledValue
    }

    val normalValue = this.toLong() * 100
    val normalDoubleValue = normalValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(normalDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return normal value: $normalValue")
        }
        return normalValue
    }

    val flooredValue = floor(this * 100).toLong()
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble, return floored value: $flooredValue")
    }

    return flooredValue
}

private fun Long?.getRealValueFromDB(): Double = if( this != null ) this / 100.0 else 0.0