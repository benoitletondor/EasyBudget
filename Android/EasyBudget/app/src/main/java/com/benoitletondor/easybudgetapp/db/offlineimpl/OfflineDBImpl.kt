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

package com.benoitletondor.easybudgetapp.db.offlineimpl

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.kizitonwose.calendar.core.atStartOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class OfflineDBImpl(private val roomDB: RoomDB) : DB {
    private val onChangeMutableFlow = MutableSharedFlow<Unit>()
    override val onChangeFlow: Flow<Unit> = onChangeMutableFlow

    override fun ensureDBCreated() {
        roomDB.openHelper.writableDatabase
    }

    override suspend fun triggerForceWriteToDisk() {
        roomDB.expenseDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newId = roomDB.expenseDao().persistExpense(expense.toExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return expense.copy(id = newId)
    }

    override suspend fun getDataForMonth(yearMonth: YearMonth): DataForMonth {
        val startDate = yearMonth.atStartOfMonth().minusDays(DataForMonth.numberOfLeewayDays)
        val endDate = yearMonth.atEndOfMonth().plusDays(DataForMonth.numberOfLeewayDays)

        var balance = roomDB.expenseDao().getBalanceForDay(startDate.minusDays(1)).getRealValueFromDB()
        var checkedBalance = roomDB.expenseDao().getCheckedBalanceForDay(startDate.minusDays(1)).getRealValueFromDB()

        val expenses = roomDB.expenseDao().getExpensesBetweenDays(startDate, endDate).toExpenses(this)
        val daysData = mutableMapOf<LocalDate, DataForDay>()

        var dayDate = startDate
        while (!dayDate.isAfter(endDate)) {
            val dayData = computeDataForDay(dayDate, expenses, balance, checkedBalance)

            daysData[dayDate] = dayData
            balance = dayData.balance
            checkedBalance = dayData.checkedBalance

            dayDate = dayDate.plusDays(1)
        }

        return DataForMonth(
            month = yearMonth,
            daysData = daysData,
        )
    }

    private fun computeDataForDay(
        dayDate: LocalDate,
        expensesForMonth: List<Expense>,
        balanceBeforeDay: Double,
        checkedBalanceBeforeDay: Double,
    ): DataForDay {
        val expensesForDay = expensesForMonth.filter { it.date == dayDate }

        return DataForDay(
            day = dayDate,
            expenses = expensesForDay,
            balance = balanceBeforeDay + expensesForDay.sumOf { it.amount },
            checkedBalance = checkedBalanceBeforeDay + expensesForDay.filter { it.checked }.sumOf { it.amount },
        )
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        return roomDB.expenseDao().getExpensesForDay(dayDate).toExpenses(this)
    }

    override suspend fun getExpensesForMonth(month: YearMonth): List<Expense> {
        return roomDB.expenseDao().getExpensesBetweenDays(month.atStartOfMonth(), month.atEndOfMonth()).toExpenses(this)
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        return roomDB.expenseDao().getBalanceForDay(dayDate).getRealValueFromDB()
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        return roomDB.expenseDao().getCheckedBalanceForDay(dayDate).getRealValueFromDB()
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        return if (recurringExpense.id == null) {
            val persistedExpense = persistNewRecurringExpenseAndFlattenExpenses(recurringExpense)

            onChangeMutableFlow.emit(Unit)

            persistedExpense
        } else {
            val newId = roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())

            onChangeMutableFlow.emit(Unit)

            recurringExpense.copy(id = newId)
        }
    }

    override suspend fun updateRecurringExpenseAfterDate(
        newRecurringExpense: RecurringExpense,
        oldOccurrenceDate: LocalDate,
    ) {
        val recurringExpenseId = newRecurringExpense.id ?: throw IllegalArgumentException("updateRecurringExpenseAfterDate called with a recurring expense that has no id")

        roomDB.withTransaction {
            roomDB.expenseDao().deleteAllExpenseForRecurringExpenseAfterDateInclusive(recurringExpenseId, oldOccurrenceDate)
            persistRecurringExpense(newRecurringExpense)
            flattenExpenses(newRecurringExpense.recurringDate, newRecurringExpense)
        }

        onChangeMutableFlow.emit(Unit)
    }

    private suspend fun persistNewRecurringExpenseAndFlattenExpenses(recurringExpense: RecurringExpense): RecurringExpense {
        return roomDB.withTransaction {
            val newId = roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())
            val persistedRecurringExpense = recurringExpense.copy(
                id = newId,
            )

            flattenExpenses(persistedRecurringExpense.recurringDate, persistedRecurringExpense)

            return@withTransaction persistedRecurringExpense
        }
    }

    private suspend fun flattenExpenses(
        startDate: LocalDate,
        persistedRecurringExpense: RecurringExpense
    ) {
        var currentDate = startDate
        when (persistedRecurringExpense.type) {
            RecurringExpenseType.DAILY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 365 * 5) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusDays(1)
                }
            }

            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plus(1, ChronoUnit.WEEKS)
                }
            }

            RecurringExpenseType.BI_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plus(2, ChronoUnit.WEEKS)
                }
            }

            RecurringExpenseType.TER_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plus(3, ChronoUnit.WEEKS)
                }
            }

            RecurringExpenseType.FOUR_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plus(4, ChronoUnit.WEEKS)
                }
            }

            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12 * 10) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusMonths(1)
                }
            }

            RecurringExpenseType.BI_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 6 * 25) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusMonths(2)
                }
            }

            RecurringExpenseType.TER_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 4 * 25) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusMonths(3)
                }
            }

            RecurringExpenseType.SIX_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 2 * 25) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusMonths(6)
                }
            }

            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    roomDB.expenseDao().persistExpense(
                        Expense(
                            persistedRecurringExpense.title,
                            persistedRecurringExpense.amount,
                            currentDate,
                            false,
                            AssociatedRecurringExpense(
                                persistedRecurringExpense,
                                persistedRecurringExpense.recurringDate
                            )
                        ).toExpenseEntity()
                    )

                    currentDate = currentDate.plusYears(1)
                }
            }
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction {
        if( recurringExpense.id == null ) {
            throw IllegalArgumentException("deleteRecurringExpense called with a recurring expense that has no id")
        }

        val allExpenses = roomDB.withTransaction {
            val expenses = getAllExpenseForRecurringExpense(recurringExpense)

            deleteAllExpenseForRecurringExpense(recurringExpense)
            roomDB.expenseDao().deleteRecurringExpense(recurringExpense.toRecurringExpenseEntity())

            return@withTransaction expenses
        }

        onChangeMutableFlow.emit(Unit)

        return {
            roomDB.withTransaction {
                roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())
                for(expense in allExpenses) {
                    roomDB.expenseDao().persistExpense(expense.toExpenseEntity())
                }
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    override suspend fun deleteExpense(expense: Expense): RestoreAction {
        if( expense.id == null ) {
            throw IllegalArgumentException("deleteExpense called with an expense that has no id")
        }

        roomDB.expenseDao().deleteExpense(expense.toExpenseEntity())

        onChangeMutableFlow.emit(Unit)

        return {
            roomDB.expenseDao().persistExpense(expense.toExpenseEntity())

            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpense called with a recurring expense that has no id")

        roomDB.expenseDao().deleteAllExpenseForRecurringExpense(recurringExpenseId)
    }

    private suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpenseForRecurringExpense called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpenseForRecurringExpense(recurringExpenseId).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): RestoreAction {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseFromDate called with a recurring expense that has no id")

        val allExpenses = roomDB.withTransaction {
            val expenses = getAllExpensesForRecurringExpenseAfterDate(recurringExpense, afterDate)

            roomDB.expenseDao().deleteAllExpenseForRecurringExpenseAfterDate(recurringExpenseId, afterDate)

            return@withTransaction expenses
        }

        onChangeMutableFlow.emit(Unit)

        return {
            roomDB.withTransaction {
                for(expense in allExpenses) {
                    roomDB.expenseDao().persistExpense(expense.toExpenseEntity())
                }
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun getAllExpensesForRecurringExpenseAfterDate(recurringExpense: RecurringExpense, afterDate: LocalDate): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseAfterDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseAfterDate(recurringExpenseId, afterDate).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): RestoreAction {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        val allExpenses = roomDB.withTransaction {
            val expenses = getAllExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

            roomDB.expenseDao().deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate)

            return@withTransaction expenses
        }

        onChangeMutableFlow.emit(Unit)

        return {
            roomDB.withTransaction {
                for(expense in allExpenses) {
                    roomDB.expenseDao().persistExpense(expense.toExpenseEntity())
                }
            }

            onChangeMutableFlow.emit(Unit)
        }
    }

    private suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): List<Expense> {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate).toExpenses(this)
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpense: RecurringExpense, beforeDate: LocalDate): Boolean {
        val recurringExpenseId = recurringExpense.id ?: throw IllegalArgumentException("hasExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().hasExpensesForRecurringExpenseBeforeDate(recurringExpenseId, beforeDate) > 0
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        return roomDB.expenseDao().findRecurringExpenseForId(recurringExpenseId)?.toRecurringExpense()
    }

    override suspend fun getOldestExpense(): Expense? {
        return roomDB.expenseDao().getOldestExpense()?.toExpense(this)
    }

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate) {
        roomDB.expenseDao().markAllEntriesAsChecked(beforeDate)

        onChangeMutableFlow.emit(Unit)
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
    checked,
    associatedRecurringExpense?.recurringExpense?.id,
)

private fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity (
    id,
    title,
    amount.getDBValue(),
    recurringDate,
    modified,
    type.name
)