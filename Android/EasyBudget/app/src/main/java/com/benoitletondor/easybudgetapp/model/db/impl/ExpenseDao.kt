package com.benoitletondor.easybudgetapp.model.db.impl

import androidx.room.*
import com.benoitletondor.easybudgetapp.model.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.model.db.impl.entity.RecurringExpenseEntity
import java.util.*

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistExpense(expenseEntity: ExpenseEntity): Long

    @Query("SELECT COUNT(*) FROM expense WHERE date >= :dayStartDate AND date <= :dayEndDate LIMIT 1")
    suspend fun hasExpenseForDay(dayStartDate: Date, dayEndDate: Date): Int

    @Query("SELECT * FROM expense WHERE date >= :dayStartDate AND date <= :dayEndDate")
    suspend fun getExpensesForDay(dayStartDate: Date, dayEndDate: Date): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date >= :monthStartDate AND date <= :monthEndDate")
    suspend fun getExpensesForMonth(monthStartDate: Date, monthEndDate: Date): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expense WHERE date <= :dayEndDate")
    suspend fun getBalanceForDay(dayEndDate: Date): Long?

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

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date > :fromDate")
    suspend fun deleteAllExpenseForRecurringExpenseFromDate(recurringExpenseId: Long, fromDate: Date)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date > :fromDate")
    suspend fun getAllExpensesForRecurringExpenseFromDate(recurringExpenseId: Long, fromDate: Date): List<ExpenseEntity>

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate")
    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: Date)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate")
    suspend fun getAllExpensesForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: Date): List<ExpenseEntity>

    @Query("SELECT count(*) FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate LIMIT 1")
    suspend fun hasExpensesForRecurringExpenseBeforeDate(recurringExpenseId: Long, beforeDate: Date): Int

    @Query("SELECT * FROM monthlyexpense WHERE _expense_id = :recurringExpenseId LIMIT 1")
    suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpenseEntity?
}