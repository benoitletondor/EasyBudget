/*
 *   Copyright 2024 Benoit LETONDOR
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

import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface DB {
    val onChangeFlow: Flow<Unit>

    fun ensureDBCreated()

    suspend fun triggerForceWriteToDisk()

    suspend fun persistExpense(expense: Expense): Expense

    suspend fun getDataForMonth(yearMonth: YearMonth): DataForMonth

    suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense>

    suspend fun getExpensesForMonth(month: YearMonth): List<Expense>

    suspend fun getBalanceForDay(dayDate: LocalDate): Double

    suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double

    suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense

    suspend fun updateRecurringExpenseAfterDate(newRecurringExpense: RecurringExpense, oldOccurrenceDate: LocalDate)

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction

    suspend fun deleteExpense(expense: Expense): RestoreAction

    suspend fun deleteAllExpenseForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): RestoreAction

    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): RestoreAction

    suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): Boolean

    suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense?

    suspend fun getOldestExpense(): Expense?

    suspend fun markAllEntriesAsChecked(beforeDate: LocalDate)
}