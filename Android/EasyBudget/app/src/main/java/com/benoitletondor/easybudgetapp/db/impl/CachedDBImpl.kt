/*
 *   Copyright 2021 Benoit LETONDOR
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

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.Logger
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.Executor

@Suppress("DeferredResultUnused")
class CachedDBImpl(private val wrappedDB: DB,
                   private val cacheStorage: CacheDBStorage,
                   private val executor: Executor) : DB {

    override fun ensureDBCreated() {
        wrappedDB.ensureDBCreated()
    }

    override suspend fun triggerForceWriteToDisk() {
        wrappedDB.triggerForceWriteToDisk()
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newExpense = wrappedDB.persistExpense(expense)

        wipeCache()

        return newExpense
    }

    override suspend fun hasExpenseForDay(dayDate: Date): Boolean {
        val expensesForDay = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate.cleaned()]
        }

        return if (expensesForDay == null) {
            executor.execute(CacheExpensesForMonthRunnable(dayDate, this, cacheStorage))
            wrappedDB.hasExpenseForDay(dayDate)
        } else {
            expensesForDay.isNotEmpty()
        }
    }

    override suspend fun getExpensesForDay(dayDate: Date): List<Expense> {
        val cached = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate.cleaned()]
        }

        if( cached != null ) {
            return cached
        } else {
            executor.execute(CacheExpensesForMonthRunnable(dayDate, this, cacheStorage))
        }

        return wrappedDB.getExpensesForDay(dayDate)
    }

    private suspend fun getExpensesForDayWithoutCache(dayDate: Date)
        = wrappedDB.getExpensesForDay(dayDate)

    override suspend fun getExpensesForMonth(monthStartDate: Date): List<Expense>
        = wrappedDB.getExpensesForMonth(monthStartDate)

    override suspend fun getBalanceForDay(dayDate: Date): Double {
        val cached = synchronized(cacheStorage.balances) {
            cacheStorage.balances[dayDate.cleaned()]
        }

        if( cached != null ) {
            return cached
        } else {
            executor.execute(CacheBalanceForMonthRunnable(dayDate, this, cacheStorage))
        }

        return wrappedDB.getBalanceForDay(dayDate)
    }

    override suspend fun getCheckedBalanceForDay(dayDate: Date): Double {
        val cached = synchronized(cacheStorage.checkedBalances) {
            cacheStorage.checkedBalances[dayDate.cleaned()]
        }

        if( cached != null ) {
            return cached
        } else {
            executor.execute(CacheCheckedBalanceForMonthRunnable(dayDate, this, cacheStorage))
        }

        return wrappedDB.getCheckedBalanceForDay(dayDate)
    }

    private suspend fun getBalanceForDayWithoutCache(dayDate: Date): Double
        = wrappedDB.getBalanceForDay(dayDate)

    private suspend fun getCheckedBalanceForDayWithoutCache(dayDate: Date): Double
        = wrappedDB.getCheckedBalanceForDay(dayDate)

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense
        = wrappedDB.persistRecurringExpense(recurringExpense)

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteRecurringExpense(recurringExpense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        wrappedDB.deleteExpense(expense)

        wipeCache()
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteAllExpenseForRecurringExpense(recurringExpense)

        wipeCache()
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense>
        = wrappedDB.getAllExpenseForRecurringExpense(recurringExpense)

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date) {
        wrappedDB.deleteAllExpenseForRecurringExpenseFromDate(recurringExpense, fromDate)

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date): List<Expense>
        = wrappedDB.getAllExpensesForRecurringExpenseFromDate(recurringExpense, fromDate)

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date) {
        wrappedDB.deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): List<Expense>
        = wrappedDB.getAllExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date): Boolean
        = wrappedDB.hasExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense?
        = wrappedDB.findRecurringExpenseForId(recurringExpenseId)

    override suspend fun getOldestExpense(): Expense?
        = wrappedDB.getOldestExpense()

    override suspend fun markAllEntriesAsChecked(beforeDate: Date) {
        wrappedDB.markAllEntriesAsChecked(beforeDate)

        wipeCache()
    }

    /**
     * Instantly wipe all cached data
     */
    private fun wipeCache() {
        Logger.debug("DBCache: Wipe all")

        synchronized(cacheStorage.balances) {
            cacheStorage.balances.clear()
        }

        synchronized(cacheStorage.expenses) {
            cacheStorage.expenses.clear()
        }

        synchronized(cacheStorage.checkedBalances) {
            cacheStorage.checkedBalances.clear()
        }
    }

    private class CacheExpensesForMonthRunnable(
        private val monthDate: Date,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage,
    ) : Runnable {

        override fun run() {
            // Init a calendar to the given date, setting the day of month to 1
            val cal = Calendar.getInstance()
            cal.time = monthDate.cleaned()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            synchronized(cacheStorage.expenses) {
                if (cacheStorage.expenses.containsKey(cal.time)) {
                    return
                }
            }

            // Save the month we wanna load cache for
            val month = cal.get(Calendar.MONTH)

            Logger.debug("DBCache: Caching expenses for month: $month")

            // Iterate over day of month (while are still on that month)
            while (cal.get(Calendar.MONTH) == month) {
                val date = cal.time
                val expensesForDay = runBlocking { db.getExpensesForDayWithoutCache(date) }

                synchronized(cacheStorage.expenses) {
                    cacheStorage.expenses.put(date, expensesForDay)
                }

                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            Logger.debug("DBCache: Expenses cached for month: $month")
        }

    }

    private class CacheBalanceForMonthRunnable(
        private val monthDate: Date,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage,
    ) : Runnable {

        override fun run() {
            // Init a calendar to the given date, setting the day of month to 1
            val cal = Calendar.getInstance()
            cal.time = monthDate.cleaned()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            synchronized(cacheStorage.balances) {
                if (cacheStorage.balances.containsKey(cal.time)) {
                    return
                }
            }

            // Save the month we wanna load cache for
            val month = cal.get(Calendar.MONTH)

            Logger.debug("DBCache: Caching balance for month: $month")

            // Iterate over day of month (while are still on that month)
            while (cal.get(Calendar.MONTH) == month) {
                val date = cal.time
                val balanceForDay = runBlocking { db.getBalanceForDayWithoutCache(date) }

                synchronized(cacheStorage.balances) {
                    cacheStorage.balances.put(date, balanceForDay)
                }

                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            Logger.debug("DBCache: Balance cached for month: $month")
        }
    }

    private class CacheCheckedBalanceForMonthRunnable(
        private val monthDate: Date,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage,
    ) : Runnable {

        override fun run() {
            // Init a calendar to the given date, setting the day of month to 1
            val cal = Calendar.getInstance()
            cal.time = monthDate.cleaned()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            synchronized(cacheStorage.checkedBalances) {
                if (cacheStorage.checkedBalances.containsKey(cal.time)) {
                    return
                }
            }

            // Save the month we wanna load cache for
            val month = cal.get(Calendar.MONTH)

            Logger.debug("DBCache: Caching checked balance for month: $month")

            // Iterate over day of month (while are still on that month)
            while (cal.get(Calendar.MONTH) == month) {
                val date = cal.time
                val balanceForDay = runBlocking { db.getCheckedBalanceForDayWithoutCache(date) }

                synchronized(cacheStorage.checkedBalances) {
                    cacheStorage.checkedBalances.put(date, balanceForDay)
                }

                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            Logger.debug("DBCache: Checked balance cached for month: $month")
        }
    }
}

interface CacheDBStorage {
    val expenses: MutableMap<Date, List<Expense>>
    val balances: MutableMap<Date, Double>
    val checkedBalances: MutableMap<Date, Double>
}

private fun Date.cleaned(): Date {
    val cal = Calendar.getInstance()
    cal.time = this

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    return cal.time
}