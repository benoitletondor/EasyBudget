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

package com.benoitletondor.easybudgetapp.db

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import java.io.Closeable
import java.util.*

interface DB : Closeable {
    fun ensureDBCreated()

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