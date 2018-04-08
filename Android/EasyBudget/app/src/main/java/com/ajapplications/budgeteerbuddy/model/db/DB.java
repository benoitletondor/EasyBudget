/*
 *   Copyright 2015 Benoit LETONDOR
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

package com.ajapplications.budgeteerbuddy.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;

import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.helper.DateHelper;
import com.ajapplications.budgeteerbuddy.helper.Logger;
import com.ajapplications.budgeteerbuddy.model.Expense;
import com.ajapplications.budgeteerbuddy.model.RecurringExpense;
import com.ajapplications.budgeteerbuddy.model.RecurringExpenseType;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Database wrapper and DAO
 *
 * @author Benoit LETONDOR
 */
public final class DB
{
    /**
     * The SQLLite DB
     */
    private final SQLiteDatabase database;
    /**
     * Saved context
     */
    private final Context context;

// -------------------------------------------->

    /**
     * Create and open a new DB
     *
     * @param context
     * @throws SQLiteException
     */
    public DB(@NonNull Context context) throws SQLiteException
    {
        this.context = context.getApplicationContext();
        SQLiteDBHelper databaseHelper = new SQLiteDBHelper(this.context);
		database = databaseHelper.getWritableDatabase();
	}

    /**
     * Close the DB, no call to other methods should be made after this method
     */
    public void close()
    {
        try
        {
            database.close();
        }
        catch (Exception e)
        {
            Logger.error("Error while closing SQLite DB", e);
        }
    }

    /**
     * Clear all DB content (<b>for test purpose</b>)
     */
    @VisibleForTesting
    public void clearDB()
    {
        database.delete(SQLiteDBHelper.TABLE_EXPENSE, null, null);
        database.delete(SQLiteDBHelper.TABLE_RECURRING_EXPENSE, null, null);
    }

// -------------------------------------------->

    /**
     * Add or update a one time expense into DB
     *
     * @param expense
     * @param forcePersist if true, it will be an insert even if an id already exists
     * @return true on success, false on error
     */
    public boolean persistExpense(@NonNull Expense expense, boolean forcePersist)
    {
        if( expense.getId() != null && !forcePersist )
        {
            int rowsAffected = database.update(SQLiteDBHelper.TABLE_EXPENSE, generateContentValuesForExpense(expense), SQLiteDBHelper.COLUMN_EXPENSE_DB_ID+"="+expense.getId(), null);
            if( rowsAffected > 0 )
            {
                // Refresh cache for day
                DBCache.getInstance(context).wipeAll(); // FIXME we should refresh for the new expense date & the old one
            }

            return rowsAffected == 1;
        }
        else
        {
            long id = database.insert(SQLiteDBHelper.TABLE_EXPENSE, null, generateContentValuesForExpense(expense));

            if( id > 0 )
            {
                // Refresh cache for day
                DBCache.getInstance(context).refreshForDay(this, expense.getDate());

                expense.setId(id);
                return true;
            }
        }

        return false;
    }

    /**
     * Add or update a one time expense into DB
     *
     * @param expense
     * @return true on success, false on error
     */
    public boolean persistExpense(@NonNull Expense expense)
    {
        return persistExpense(expense, false);
    }

    /**
     * Check if an expense is set to the given day
     *
     * @param day
     * @return
     */
    public boolean hasExpensesForDay(@NonNull Date day)
    {
        Pair<Long, Long> range = DateHelper.getTimestampRangeForDay(day);
        Date gmt = DateHelper.cleanGMTDate(day);

        // Check cache
        Boolean hasExpensesCached = DBCache.getInstance(context).hasExpensesForDay(gmt);
        if( hasExpensesCached != null )
        {
            return hasExpensesCached;
        }

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + SQLiteDBHelper.TABLE_EXPENSE + " WHERE " + SQLiteDBHelper.COLUMN_EXPENSE_DATE + " >= " + range.first + " AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE + " <= "+ range.second, null);

            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Get all one time expense for a day
     *
     * @param date
     * @param fromCache should we use cache or not
     * @return
     */
    @NonNull
    protected List<Expense> getExpensesForDay(@NonNull Date date, boolean fromCache)
    {
        Pair<Long, Long> range = DateHelper.getTimestampRangeForDay(date);
        Date gmt = DateHelper.cleanGMTDate(date);

        // Check cache
        if( fromCache )
        {
            List<Expense> cachedExpenses = DBCache.getInstance(context).getExpensesForDay(gmt);
            if( cachedExpenses != null )
            {
                return cachedExpenses;
            }
        }

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_DATE + " >= " + range.first + " AND " + SQLiteDBHelper.COLUMN_EXPENSE_DATE + " <= " + range.second, null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                try
                {
                    expenses.add(ExpenseFromCursor(cursor, getRecurringExpenseForExpenseCursor(cursor)));
                }
                catch (Exception e)
                {
                    Logger.error(false, "Error occurred querying DB for expense for a day", e);
                }
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Get all one time expense for a day
     *
     * @param date
     * @return
     */
    @NonNull
    public List<Expense> getExpensesForDay(@NonNull Date date)
    {
        return getExpensesForDay(date, true);
    }

    /**
     * Get all the expenses for the given month ordered by date
     *
     * @param firstDate first day of the month at 00:00:000
     * @return expenses for the given month
     */
    @NonNull
    public List<Expense> getExpensesForMonth(@NonNull Date firstDate)
    {
        Pair<Long, Long> firstDateRange = DateHelper.getTimestampRangeForDay(firstDate);

        Calendar cal = Calendar.getInstance();
        cal.setTime(firstDate);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);

        Pair<Long, Long> lastDateRange = DateHelper.getTimestampRangeForDay(cal.getTime());

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_DATE + " >= " + firstDateRange.first + " AND " + SQLiteDBHelper.COLUMN_EXPENSE_DATE + " <= " + lastDateRange.second+" ORDER BY "+SQLiteDBHelper.COLUMN_EXPENSE_DATE, null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                try
                {
                    expenses.add(ExpenseFromCursor(cursor, getRecurringExpenseForExpenseCursor(cursor)));
                }
                catch (Exception e)
                {
                    Logger.error(false, "Error occurred querying DB for expense for a month", e);
                }
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Get a sum of all amount of expenses until the given day
     *
     * @param day
     * @param fromCache should we use DBCache
     * @return
     */
    protected double getBalanceForDay(@NonNull Date day, boolean fromCache)
    {
        Pair<Long, Long> range = DateHelper.getTimestampRangeForDay(day);
        Date gmt = DateHelper.cleanGMTDate(day);

        // Check cache
        if( fromCache )
        {
            Double cachedBalance = DBCache.getInstance(context).getBalanceForDay(gmt);
            if( cachedBalance != null )
            {
                return cachedBalance;
            }
        }

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT SUM(" + SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT + ") FROM " + SQLiteDBHelper.TABLE_EXPENSE + " WHERE " + SQLiteDBHelper.COLUMN_EXPENSE_DATE + " <= " + range.second, null);

            if(cursor.moveToFirst())
            {
                int value = cursor.getInt(0);
                return (double) value / 100.d;
            }

            return 0;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Get a sum of all amount of expenses until the given day
     *
     * @param day
     * @return
     */
    public double getBalanceForDay(@NonNull Date day)
    {
        return getBalanceForDay(day, true);
    }

    /**
     * Add a recurring expense
     *
     * @param expense
     * @return true on success, false on error
     */
    public boolean addRecurringExpense(@NonNull RecurringExpense expense)
    {
        long id = database.insert(SQLiteDBHelper.TABLE_RECURRING_EXPENSE, null, generateContentValuesForRecurringExpense(expense));

        if( id > 0 )
        {
            expense.setId(id);
            return true;
        }

        return false;
    }

    /**
     * Get all recurring expenses
     *
     * @return
     */
    @NonNull
    public List<RecurringExpense> getAllRecurringExpenses()
    {
        Cursor cursor = null;
        try
        {
            List<RecurringExpense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_RECURRING_EXPENSE, null, null, null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(recurringExpenseFromCursor(cursor));
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Delete this recurring expense
     *
     * @param recurringExpense
     * @return true on success, false on error
     */
    public boolean deleteRecurringExpense(@NonNull RecurringExpense recurringExpense)
    {
        return database.delete(SQLiteDBHelper.TABLE_RECURRING_EXPENSE, SQLiteDBHelper.COLUMN_RECURRING_DB_ID +"="+ recurringExpense.getId(), null) > 0;
    }

    /**
     * Delete this expense
     *
     * @param expense
     * @return true on success, false on error
     */
    public boolean deleteExpense(@NonNull Expense expense)
    {
        boolean delete = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_DB_ID+"="+expense.getId(), null) > 0;

        if( delete )
        {
            // Refresh cache for day
            DBCache.getInstance(context).refreshForDay(this, expense.getDate());
        }

        return delete;
    }

    /**
     * Delete all expense for this recurring expense
     *
     * @param recurringExpense
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForRecurringExpense(@NonNull RecurringExpense recurringExpense)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return deleted;
    }

    /**
     * Get all expenses associated with this recurring expense
     *
     * @param recurringExpense
     * @return
     */
    public List<Expense> getAllExpenseForRecurringExpense(@NonNull RecurringExpense recurringExpense)
    {
        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor, recurringExpense));
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Delete all expense for this recurring expense from the given date (not included)
     *
     * @param recurringExpense
     * @param fromDate
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForRecurringExpenseFromDate(@NonNull RecurringExpense recurringExpense, @NonNull Date fromDate)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+">"+fromDate.getTime(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return  deleted;
    }

    /**
     * Retrieve all expenses associated with this recurring expense happening after the given date (not included)
     *
     * @param recurringExpense
     * @param fromDate
     * @return
     */
    public List<Expense> getAllExpensesForRecurringExpenseFromDate(@NonNull RecurringExpense recurringExpense, @NonNull Date fromDate)
    {
        fromDate = DateHelper.cleanDate(fromDate);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+">"+fromDate.getTime(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor, recurringExpense));
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Delete all expense for this recurring expense before the given date (excluded)
     *
     * @param recurringExpense
     * @param toDate
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForRecurringExpenseBeforeDate(@NonNull RecurringExpense recurringExpense, @NonNull Date toDate)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return deleted;
    }

    /**
     * Check if there are expenses before this date for the given recurring expense
     *
     * @param recurringExpense
     * @param toDate
     * @return
     */
    public boolean hasExpensesForRecurringExpenseBeforeDate(@NonNull RecurringExpense recurringExpense, @NonNull Date toDate)
    {
        toDate = DateHelper.cleanDate(toDate);

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT COUNT(*) FROM "+SQLiteDBHelper.TABLE_EXPENSE+" WHERE "+SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime()+" LIMIT 1", null);

            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Retrieve all expenses associated with this recurring expense happening before the given date
     *
     * @param recurringExpense
     * @param toDate
     * @return
     */
    public List<Expense> getAllExpensesForRecurringExpenseBeforeDate(@NonNull RecurringExpense recurringExpense, @NonNull Date toDate)
    {
        toDate = DateHelper.cleanDate(toDate);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID +"="+ recurringExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor, recurringExpense));
            }

            return expenses;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

    /**
     * Find the recurring expense for the given ID
     *
     * @param id
     * @return recurring expense if found, null otherwise
     */
    @Nullable
    public RecurringExpense findRecurringExpenseForId(long id)
    {
        Cursor cursor = null;
        try
        {
            cursor = database.query(SQLiteDBHelper.TABLE_RECURRING_EXPENSE, null, SQLiteDBHelper.COLUMN_RECURRING_DB_ID + " = " + id, null, null, null, null, "1");

            if(cursor.moveToFirst())
            {
                return recurringExpenseFromCursor(cursor);
            }

            return null;
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }
    }

// -------------------------------------------->

    /**
     * Deserialize an expense from DB
     *
     * @param cursor
     * @return
     */
    @NonNull
    private static Expense ExpenseFromCursor(@NonNull Cursor cursor, @Nullable RecurringExpense recurringExpense)
    {
        return new Expense
        (
            cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_DB_ID)),
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_TITLE)),
            (double) cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT)) / 100.d,
            new Date(cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_DATE))),
            recurringExpense
        );
    }

    /**
     * Find the recurring expense associated with an expense cursor
     *
     * @param cursor
     * @return
     */
    @Nullable
    private RecurringExpense getRecurringExpenseForExpenseCursor(@NonNull Cursor cursor)
    {
        long recurringId = 0;
        try
        {
            recurringId = cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID));
        }
        catch(Exception e)
        {
            // Exception can be thrown on null depending on impl.
        }

        if( recurringId > 0 )
        {
            return findRecurringExpenseForId(recurringId);
        }

        return null;
    }

    /**
     * Generate serialized values for an expense
     *
     * @param expense
     * @return
     */
    @NonNull
    private static ContentValues generateContentValuesForExpense(@NonNull Expense expense)
    {
        final ContentValues values = new ContentValues();

        if( expense.getId() != null )
        {
            values.put(SQLiteDBHelper.COLUMN_EXPENSE_DB_ID, expense.getId());
        }

        values.put(SQLiteDBHelper.COLUMN_EXPENSE_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_EXPENSE_DATE, expense.getDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT, CurrencyHelper.getDBValueForDouble(expense.getAmount()));

        if( expense.isRecurring() )
        {
            assert expense.getAssociatedRecurringExpense() != null;
            values.put(SQLiteDBHelper.COLUMN_EXPENSE_RECURRING_ID, expense.getAssociatedRecurringExpense().getId());
        }

        return values;
    }

    /**
     * Deserialize a recurring expense from DB
     *
     * @param cursor
     * @return
     * @throws JSONException
     */
    @NonNull
    private static RecurringExpense recurringExpenseFromCursor(@NonNull Cursor cursor)
    {
        return new RecurringExpense
        (
            cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_DB_ID)),
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_TITLE)),
            (double) cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_AMOUNT)) / 100.d,
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_RECURRING_DATE))),
            RecurringExpenseType.valueOf(cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_TYPE))),
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_RECURRING_MODIFIED)) == 1
        );
    }

    /**
     * Generate serialized values for a recurring expense
     *
     * @param expense
     * @return
     * @throws JSONException
     */
    @NonNull
    private static ContentValues generateContentValuesForRecurringExpense(@NonNull RecurringExpense expense)
    {
        final ContentValues values = new ContentValues();

        if( expense.getId() != null )
        {
            values.put(SQLiteDBHelper.COLUMN_RECURRING_DB_ID, expense.getId());
        }

        values.put(SQLiteDBHelper.COLUMN_RECURRING_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_RECURRING_RECURRING_DATE, expense.getRecurringDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_RECURRING_AMOUNT, CurrencyHelper.getDBValueForDouble(expense.getAmount()));
        values.put(SQLiteDBHelper.COLUMN_RECURRING_TYPE, expense.getType().name());
        values.put(SQLiteDBHelper.COLUMN_RECURRING_MODIFIED, expense.isModified() ? 1 : 0);

        return values;
    }
}
