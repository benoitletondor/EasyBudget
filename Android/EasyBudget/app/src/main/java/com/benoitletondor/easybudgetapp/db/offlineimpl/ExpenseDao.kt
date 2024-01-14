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

import androidx.room.*
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.RecurringExpenseEntity
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery
import java.time.LocalDate

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistExpense(expenseEntity: ExpenseEntity): Long

    @Query("SELECT COUNT(*) FROM expense WHERE date = :dayDate LIMIT 1")
    suspend fun hasExpenseForDay(dayDate: LocalDate): Int

    @Query("SELECT COUNT(*) FROM expense WHERE date = :dayDate AND checked = 0 LIMIT 1")
    suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Int

    @Query("SELECT * FROM expense WHERE date = :dayDate")
    suspend fun getExpensesForDay(dayDate: LocalDate): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date >= :startDate AND date <= :endDate")
    suspend fun getExpensesBetweenDays(startDate: LocalDate, endDate: LocalDate): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expense WHERE date <= :dayDate")
    suspend fun getBalanceForDay(dayDate: LocalDate): Long?

    @Query("SELECT SUM(amount) FROM expense WHERE date <= :dayDate AND checked")
    suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistRecurringExpense(recurringExpenseEntity: RecurringExpenseEntity): Long

    @Delete
    suspend fun deleteRecurringExpense(recurringExpenseEntity: RecurringExpenseEntity)

    @Delete
    suspend fun deleteExpense(expenseEntity: ExpenseEntity)

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId")
    suspend fun deleteAllExpenseForRecurringExpense(recurringExpenseId: Long)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId")
    suspend fun getAllExpenseForRecurringExpense(recurringExpenseId: Long): List<ExpenseEntity>

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date > :afterDate")
    suspend fun deleteAllExpenseForRecurringExpenseAfterDate(recurringExpenseId: Long, afterDate: LocalDate)

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date >= :afterDate")
    suspend fun deleteAllExpenseForRecurringExpenseAfterDateInclusive(recurringExpenseId: Long, afterDate: LocalDate)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date > :afterDate")
    suspend fun getAllExpensesForRecurringExpenseAfterDate(recurringExpenseId: Long, afterDate: LocalDate): List<ExpenseEntity>

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate")
    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: LocalDate)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate")
    suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: LocalDate): List<ExpenseEntity>

    @Query("SELECT count(*) FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate LIMIT 1")
    suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: LocalDate): Int

    @Query("SELECT * FROM monthlyexpense WHERE _expense_id = :recurringExpenseId LIMIT 1")
    suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpenseEntity?

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int

    @Query("SELECT * FROM expense ORDER BY date LIMIT 1")
    suspend fun getOldestExpense(): ExpenseEntity?

    @Query("UPDATE expense SET checked = 1 WHERE date < :beforeDate")
    suspend fun markAllEntriesAsChecked(beforeDate: LocalDate)
}