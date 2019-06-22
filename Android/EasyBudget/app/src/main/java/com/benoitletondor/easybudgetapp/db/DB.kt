package com.benoitletondor.easybudgetapp.db

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import java.io.Closeable
import java.util.*

interface DB : Closeable {
    suspend fun persistExpense(expense: Expense): Expense

    suspend fun hasExpenseForDay(dayDate: Date): Boolean

    suspend fun getExpensesForDay(dayDate: Date): List<Expense>

    suspend fun getExpensesForMonth(monthStartDate: Date): List<Expense>

    suspend fun getBalanceForDay(dayDate: Date): Double

    suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun deleteExpense(expense: Expense)

    suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date)

    suspend fun getAllExpensesForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date)

    suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): List<Expense>

    suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): Boolean

    suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense?
}