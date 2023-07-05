package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import java.time.LocalDate

class OnlineDBImpl : DB {


    override fun ensureDBCreated() {
        TODO("Not yet implemented")
    }

    override suspend fun triggerForceWriteToDisk() {
        TODO("Not yet implemented")
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        TODO("Not yet implemented")
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        TODO("Not yet implemented")
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        TODO("Not yet implemented")
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteExpense(expense: Expense) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpensesForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        TODO("Not yet implemented")
    }

    override suspend fun getOldestExpense(): Expense? {
        TODO("Not yet implemented")
    }

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate) {
        TODO("Not yet implemented")
    }
}