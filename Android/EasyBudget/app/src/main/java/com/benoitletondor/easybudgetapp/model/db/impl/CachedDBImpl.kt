package com.benoitletondor.easybudgetapp.model.db.impl

import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

@Suppress("DeferredResultUnused")
class CachedDBImpl(private val wrappedDB: DB,
                   private val cacheStorage: CacheDBStorage,
                   private val executor: Executor) : DB, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = executor.asCoroutineDispatcher() + SupervisorJob()

    override fun close() {
        wrappedDB.close()
        cancel()
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newExpense = wrappedDB.persistExpense(expense)

        if( expense.id != null ) {
            wipeAll()
        } else {
            refreshCacheForDay(expense.date)
        }

        return newExpense
    }

    override suspend fun hasExpenseForDay(dayDate: Date): Boolean {
        val expensesForDay = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate.cleanGMTDate()]
        }

        return if (expensesForDay == null) {
            cacheExpensesMonth(dayDate.cleanGMTDate())
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
            cacheExpensesMonth(dayDate.cleanGMTDate())
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
            cacheBalanceForMonth(dayDate.cleanGMTDate())
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

        refreshCacheForDay(expense.date)
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteAllExpenseForRecurringExpense(recurringExpense)

        wipeAll()
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense>
        = wrappedDB.getAllExpenseForRecurringExpense(recurringExpense)

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date) {
        wrappedDB.deleteAllExpenseForRecurringExpenseFromDate(recurringExpense, fromDate)

        wipeAll()
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(recurringExpense: RecurringExpense, fromDate: Date): List<Expense>
        = wrappedDB.getAllExpensesForRecurringExpenseFromDate(recurringExpense, fromDate)

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: Date) {
        wrappedDB.deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

        wipeAll()
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
    private fun wipeAll() {
        synchronized(cacheStorage.balances) {
            cacheStorage.balances.clear()
        }

        synchronized(cacheStorage.expenses) {
            cacheStorage.expenses.clear()
        }
    }

    /**
     * Instantly refresh cached data for the given day
     *
     * @param date cleaned date for the day
     */
    private fun refreshCacheForDay(date: Date) {
        async {
            synchronized(cacheStorage.balances) {
                cacheStorage.balances.clear() // TODO be smarter than delete all ?
            }

            val loadedExpenses = getExpensesForDayWithoutCache(date)
            synchronized(cacheStorage.expenses) {
                cacheStorage.expenses.put(date.cleanGMTDate(), loadedExpenses)
            }
        }
    }

    private fun cacheExpensesMonth(monthDate: Date) {
        async {
            // Init a calendar to the given date, setting the day of month to 1
            val cal = Calendar.getInstance()
            cal.time = monthDate.clean()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            synchronized(cacheStorage.expenses) {
                if (cacheStorage.expenses.containsKey(cal.time.cleanGMTDate())) {
                    return@async
                }
            }

            // Save the month we wanna load cache for
            val month = cal.get(Calendar.MONTH)

            // Iterate over day of month (while are still on that month)
            while (cal.get(Calendar.MONTH) == month) {
                val date = cal.time
                val expensesForDay = getExpensesForDayWithoutCache(date)

                synchronized(cacheStorage.expenses) {
                    cacheStorage.expenses.put(date.cleanGMTDate(), expensesForDay)
                }

                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    private fun cacheBalanceForMonth(monthDate: Date) {
        async {
            // Init a calendar to the given date, setting the day of month to 1
            val cal = Calendar.getInstance()
            cal.time = monthDate.clean()
            cal.set(Calendar.DAY_OF_MONTH, 1)

            synchronized(cacheStorage.balances) {
                if (cacheStorage.balances.containsKey(cal.time.cleanGMTDate())) {
                    return@async
                }
            }

            // Save the month we wanna load cache for
            val month = cal.get(Calendar.MONTH)

            // Iterate over day of month (while are still on that month)
            while (cal.get(Calendar.MONTH) == month) {
                val date = cal.time
                val balanceForDay = getBalanceForDayWithoutCache(date)

                synchronized(cacheStorage.balances) {
                    cacheStorage.balances.put(date.cleanGMTDate(), balanceForDay)
                }

                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
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

}

interface CacheDBStorage {
    val expenses: MutableMap<Date, List<Expense>>
    val balances: MutableMap<Date, Double>
}