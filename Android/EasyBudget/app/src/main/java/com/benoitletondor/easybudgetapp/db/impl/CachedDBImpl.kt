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

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.Logger
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import java.util.*
import java.util.concurrent.Executor

@Suppress("DeferredResultUnused")
class CachedDBImpl(private val wrappedDB: DB,
                   private val cacheStorage: CacheDBStorage,
                   private val executor: Executor) : DB {

    override fun close() {
        wrappedDB.close()
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newExpense = wrappedDB.persistExpense(expense)

        if( expense.id != null ) {
            wipeCache()
        } else {
            executor.execute(CacheForDay(expense.date))
        }

        return newExpense
    }

    override suspend fun hasExpenseForDay(dayDate: Date): Boolean {
        val expensesForDay = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate.cleanGMTDate()]
        }

        return if (expensesForDay == null) {
            executor.execute(CacheExpensesForMonthRunnable(dayDate.cleanGMTDate()))
            wrappedDB.hasExpenseForDay(dayDate)
        } else {
            expensesForDay.isNotEmpty()
        }
    }

    override suspend fun getExpensesForDay(dayDate: Date): List<Expense> {
        val cached = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate.cleanGMTDate()]
        }

        if( cached != null ) {
            return cached
        } else {
            executor.execute(CacheExpensesForMonthRunnable(dayDate.cleanGMTDate()))
        }

        return wrappedDB.getExpensesForDay(dayDate)
    }

    private suspend fun getExpensesForDayWithoutCache(dayDate: Date)
        = wrappedDB.getExpensesForDay(dayDate)

    override suspend fun getExpensesForMonth(monthStartDate: Date): List<Expense>
        = wrappedDB.getExpensesForMonth(monthStartDate)

    override suspend fun getBalanceForDay(dayDate: Date): Double {
        val cached = synchronized(cacheStorage.balances) {
            cacheStorage.balances[dayDate.cleanGMTDate()]
        }

        if( cached != null ) {
            return cached
        } else {
            executor.execute(CacheBalanceForMonthRunnable(dayDate.cleanGMTDate()))
        }

        return wrappedDB.getBalanceForDay(dayDate)
    }

    private suspend fun getBalanceForDayWithoutCache(dayDate: Date): Double
        = wrappedDB.getBalanceForDay(dayDate)

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense
        = wrappedDB.persistRecurringExpense(recurringExpense)

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteRecurringExpense(recurringExpense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        wrappedDB.deleteExpense(expense)

        executor.execute(CacheForDay(expense.date))
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
    }

    private class CacheForDay(private val date: Date) : Runnable {
        private val db: CachedDBImpl by inject(CachedDBImpl::class.java)
        private val cacheStorage: CacheDBStorage by inject(CacheDBStorage::class.java)

        override fun run() {
            db.use { db ->
                Logger.debug("DBCache: Refreshing for day: $date")

                synchronized(cacheStorage.balances) {
                    cacheStorage.balances.clear() // TODO be smarter than delete all ?
                }

                val loadedExpenses = runBlocking { db.getExpensesForDayWithoutCache(date) }
                synchronized(cacheStorage.expenses) {
                    cacheStorage.expenses.put(date.cleanGMTDate(), loadedExpenses)
                }
            }
        }
    }

    private class CacheExpensesForMonthRunnable(private val monthDate: Date) : Runnable {
        private val db: CachedDBImpl by inject(CachedDBImpl::class.java)
        private val cacheStorage: CacheDBStorage by inject(CacheDBStorage::class.java)

        override fun run() {
            db.use { db ->
                // Init a calendar to the given date, setting the day of month to 1
                val cal = Calendar.getInstance()
                cal.time = monthDate.clean()
                cal.set(Calendar.DAY_OF_MONTH, 1)

                synchronized(cacheStorage.expenses) {
                    if (cacheStorage.expenses.containsKey(cal.time.cleanGMTDate())) {
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
                        cacheStorage.expenses.put(date.cleanGMTDate(), expensesForDay)
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }

                Logger.debug("DBCache: Expenses cached for month: $month")
            }
        }

    }

    private class CacheBalanceForMonthRunnable(private val monthDate: Date) : Runnable {
        private val db: CachedDBImpl by inject(CachedDBImpl::class.java)
        private val cacheStorage: CacheDBStorage by inject(CacheDBStorage::class.java)

        override fun run() {
            db.use { db ->
                // Init a calendar to the given date, setting the day of month to 1
                val cal = Calendar.getInstance()
                cal.time = monthDate.clean()
                cal.set(Calendar.DAY_OF_MONTH, 1)

                synchronized(cacheStorage.balances) {
                    if (cacheStorage.balances.containsKey(cal.time.cleanGMTDate())) {
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
                        cacheStorage.balances.put(date.cleanGMTDate(), balanceForDay)
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }

                Logger.debug("DBCache: Balance cached for month: $month")
            }
        }
    }
}

interface CacheDBStorage {
    val expenses: MutableMap<Date, List<Expense>>
    val balances: MutableMap<Date, Double>
}

private fun Date.cleanGMTDate(): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.timeZone = TimeZone.getTimeZone("GMT")

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    return cal.time
}

private fun Date.clean(): Date {
    val cal = Calendar.getInstance()
    cal.time = this

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    return cal.time
}