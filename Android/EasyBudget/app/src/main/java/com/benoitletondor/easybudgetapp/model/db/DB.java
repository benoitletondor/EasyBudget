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

package com.benoitletondor.easybudgetapp.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;

import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.DateHelper;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.model.Expense;
import com.benoitletondor.easybudgetapp.model.MonthlyExpense;

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
        database.delete(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, null);
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
                    expenses.add(ExpenseFromCursor(cursor));
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
                    expenses.add(ExpenseFromCursor(cursor));
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
     * Add a monthly expense
     *
     * @param expense
     * @return true on success, false on error
     */
    public boolean addMonthlyExpense(@NonNull MonthlyExpense expense)
    {
        long id = database.insert(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, generateContentValuesForMonthlyExpense(expense));

        if( id > 0 )
        {
            expense.setId(id);
            return true;
        }

        return false;
    }

    /**
     * Get all monthly expenses
     *
     * @return
     */
    @NonNull
    public List<MonthlyExpense> getAllMonthlyExpenses()
    {
        Cursor cursor = null;
        try
        {
            List<MonthlyExpense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, null, null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(monthlyExpenseFromCursor(cursor));
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
     * Delete this monthly expense
     *
     * @param monthlyExpense
     * @return true on success, false on error
     */
    public boolean deleteMonthlyExpense(@NonNull MonthlyExpense monthlyExpense)
    {
        return database.delete(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, SQLiteDBHelper.COLUMN_MONTHLY_DB_ID+"="+monthlyExpense.getId(), null) > 0;
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
     * Delete all expense for this monthly expense
     *
     * @param monthlyExpense
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForMonthlyExpense(@NonNull MonthlyExpense monthlyExpense)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return deleted;
    }

    /**
     * Get all expenses associated with this monthly expense
     *
     * @param monthlyExpense
     * @return
     */
    public List<Expense> getAllExpenseForMonthlyExpense(@NonNull MonthlyExpense monthlyExpense)
    {
        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor));
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
     * Delete all expense for this monthly expense from the given date (not included)
     *
     * @param monthlyExpense
     * @param fromDate
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForMonthlyExpenseFromDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date fromDate)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+">"+fromDate.getTime(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return  deleted;
    }

    /**
     * Retrieve all expenses associated with this monthly expense happening after the given date (not included)
     *
     * @param monthlyExpense
     * @param fromDate
     * @return
     */
    public List<Expense> getAllExpensesForMonthlyExpenseFromDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date fromDate)
    {
        fromDate = DateHelper.cleanDate(fromDate);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+">"+fromDate.getTime(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor));
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
     * Delete all expense for this monthly expense before the given date (excluded)
     *
     * @param monthlyExpense
     * @param toDate
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForMonthlyExpenseBeforeDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date toDate)
    {
        boolean deleted = database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime(), null) > 0;

        if( deleted )
        {
            DBCache.getInstance(context).wipeAll();
        }

        return deleted;
    }

    /**
     * Check if there are expenses before this date for the given monthly expense
     *
     * @param monthlyExpense
     * @param toDate
     * @return
     */
    public boolean hasExpensesForMonthlyExpenseBeforeDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date toDate)
    {
        toDate = DateHelper.cleanDate(toDate);

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT COUNT(*) FROM "+SQLiteDBHelper.TABLE_EXPENSE+" WHERE "+SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime()+" LIMIT 1", null);

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
     * Retrieve all expenses associated with this monthly expense happening before the given date
     *
     * @param monthlyExpense
     * @param toDate
     * @return
     */
    public List<Expense> getAllExpensesForMonthlyExpenseBeforeDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date toDate)
    {
        toDate = DateHelper.cleanDate(toDate);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(ExpenseFromCursor(cursor));
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
     * Find the monthly expense for the given ID
     *
     * @param id
     * @return monthly expense if found, null otherwise
     */
    @Nullable
    public MonthlyExpense findMonthlyExpenseForId(long id)
    {
        Cursor cursor = null;
        try
        {
            cursor = database.query(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, SQLiteDBHelper.COLUMN_MONTHLY_DB_ID + " = " + id, null, null, null, null, "1");

            if(cursor.moveToFirst())
            {
                return monthlyExpenseFromCursor(cursor);
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
    private static Expense ExpenseFromCursor(@NonNull Cursor cursor)
    {
        long monthlyId = 0;
        try
        {
            monthlyId = cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID));
        }
        catch(Exception e)
        {
            // Exception can be thrown on null depending on impl.
        }

        return new Expense
        (
            cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_DB_ID)),
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_TITLE)),
            (double)cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT)) / 100.d,
            new Date(cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_DATE))),
            monthlyId > 0 ? monthlyId : null
        );
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

        if( expense.isMonthly() )
        {
            values.put(SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID, expense.getMonthlyId());
        }

        return values;
    }

    /**
     * Deserialize a monthly expense from DB
     *
     * @param cursor
     * @return
     * @throws JSONException
     */
    @NonNull
    private static MonthlyExpense monthlyExpenseFromCursor(@NonNull Cursor cursor)
    {
        return new MonthlyExpense
        (
            cursor.getLong(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_DB_ID)),
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_TITLE)),
            (double) cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT)) / 100.d,
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_RECURRING_DATE))),
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_MODIFIED)) == 1
        );
    }

    /**
     * Generate serialized values for a monthly expense
     *
     * @param expense
     * @return
     * @throws JSONException
     */
    @NonNull
    private static ContentValues generateContentValuesForMonthlyExpense(@NonNull MonthlyExpense expense)
    {
        final ContentValues values = new ContentValues();

        if( expense.getId() != null )
        {
            values.put(SQLiteDBHelper.COLUMN_MONTHLY_DB_ID, expense.getId());
        }

        values.put(SQLiteDBHelper.COLUMN_MONTHLY_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_RECURRING_DATE, expense.getRecurringDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT, CurrencyHelper.getDBValueForDouble(expense.getAmount()));
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_MODIFIED, expense.isModified() ? 1 : 0);

        return values;
    }
}
