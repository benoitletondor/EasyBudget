package com.benoitletondor.easybudgetapp.model.db.impl

import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.db.DB
import com.benoitletondor.easybudgetapp.model.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.model.db.impl.entity.RecurringExpenseEntity
import java.util.*

class DBImpl(private val roomDB: RoomDB) : DB {

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
        val sum = roomDB.expenseDao().getBalanceForDay(endDate)

        return if( sum != null ) sum / 100.0 else 0.0
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
    CurrencyHelper.getDBValueForDouble(amount),
    date,
    associatedRecurringExpense?.id
)

private fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity (
    id,
    title,
    CurrencyHelper.getDBValueForDouble(originalAmount),
    recurringDate,
    modified,
    type.name
)

private data class DayDateRange(val dayStartDate: Date, val dayEndDate: Date)

private fun Date.getDayDatesRange(): DayDateRange {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.timeZone = TimeZone.getTimeZone("GMT")

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    cal.add(Calendar.HOUR_OF_DAY, -11)
    val start = cal.time
    cal.add(Calendar.HOUR_OF_DAY, 23)
    val end = cal.time

    return DayDateRange(start, end)
}