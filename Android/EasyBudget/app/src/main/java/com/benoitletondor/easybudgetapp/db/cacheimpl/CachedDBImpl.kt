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

package com.benoitletondor.easybudgetapp.db.cacheimpl

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

open class CachedDBImpl(
    private val wrappedDB: DB,
) : DB, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val onChangeMutableFlow = MutableSharedFlow<Unit>()
    override val onChangeFlow: Flow<Unit> = onChangeMutableFlow

    private val cacheMutex = Mutex()
    private val cachedDataForMonths = mutableMapOf<YearMonth, DataForMonth>()

    init {
        launch {
            wrappedDB.onChangeFlow
                .collect {
                    wipeCache()
                    onChangeMutableFlow.emit(Unit)
                }
        }
    }

    override fun ensureDBCreated() {
        wrappedDB.ensureDBCreated()
    }

    override suspend fun triggerForceWriteToDisk() {
        wrappedDB.triggerForceWriteToDisk()
    }

    override suspend fun persistExpense(expense: Expense): Expense
        = wrappedDB.persistExpense(expense)

    override suspend fun getDataForMonth(
        yearMonth: YearMonth,
    ): DataForMonth {
        cacheMutex.withLock {
            return cachedDataForMonths.getOrPut(yearMonth) {
                Logger.debug("DBCache: Caching data for month: $yearMonth")
                wrappedDB.getDataForMonth(yearMonth)
            }
        }
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        val dateForMonth = getDataForMonth(dayDate.yearMonth)

        return dateForMonth.daysData[dayDate]?.expenses ?: emptyList()
    }

    override suspend fun getExpensesForMonth(month: YearMonth): List<Expense> {
        val dateForMonth = getDataForMonth(month)

        return dateForMonth.daysData
            .flatMap { (day, dataForDay) ->
                if (day.yearMonth == month) {
                    dataForDay.expenses
                } else {
                    emptyList()
                }
            }
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        val dateForMonth = getDataForMonth(dayDate.yearMonth)

        return dateForMonth.daysData[dayDate]?.balance ?: 0.0
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        val dateForMonth = getDataForMonth(dayDate.yearMonth)

        return dateForMonth.daysData[dayDate]?.checkedBalance ?: 0.0
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense
        = wrappedDB.persistRecurringExpense(recurringExpense)

    override suspend fun updateRecurringExpenseAfterDate(
        newRecurringExpense: RecurringExpense,
        oldOccurrenceDate: LocalDate
    ) = wrappedDB.updateRecurringExpenseAfterDate(newRecurringExpense, oldOccurrenceDate)

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction
        = wrappedDB.deleteRecurringExpense(recurringExpense)

    override suspend fun deleteExpense(expense: Expense): RestoreAction
        = wrappedDB.deleteExpense(expense)

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): RestoreAction
        = wrappedDB.deleteAllExpenseForRecurringExpenseAfterDate(recurringExpense, afterDate)

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): RestoreAction
        = wrappedDB.deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): Boolean
        = wrappedDB.hasExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense?
        = wrappedDB.findRecurringExpenseForId(recurringExpenseId)

    override suspend fun getOldestExpense(): Expense?
        = wrappedDB.getOldestExpense()

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate)
        = wrappedDB.markAllEntriesAsChecked(beforeDate)

    private suspend fun wipeCache() {
        cacheMutex.withLock {
            Logger.debug("DBCache: Wipe all")
            cachedDataForMonths.clear()
        }
    }
}