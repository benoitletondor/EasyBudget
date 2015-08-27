package com.benoitletondor.easybudget.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.benoitletondor.easybudget.helper.DateHelper;
import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.model.Expense;
import com.benoitletondor.easybudget.model.MonthlyExpense;

import org.json.JSONException;

import java.util.ArrayList;
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

// -------------------------------------------->

    /**
     * Create and open a new DB
     *
     * @param context
     * @throws SQLiteException
     */
    public DB(@NonNull Context context) throws SQLiteException
    {
        SQLiteDBHelper databaseHelper = new SQLiteDBHelper(context.getApplicationContext());
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
     * Add a one time expense into DB
     *
     * @param expense
     * @return true on success, false on error
     */
    public boolean addExpense(@NonNull Expense expense)
    {
        long id = database.insert(SQLiteDBHelper.TABLE_EXPENSE, null, generateContentValuesForExpense(expense));

        if( id > 0 )
        {
            expense.setId(id);
            return true;
        }

        return false;
    }

    /**
     * Check if an expense is set to the given day
     *
     * @param day
     * @return
     */
    public boolean hasExpensesForDay(@NonNull Date day)
    {
        day = DateHelper.cleanDate(day);

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT COUNT(*) FROM "+SQLiteDBHelper.TABLE_EXPENSE+" WHERE "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+" = "+day.getTime(), null);

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
     * @return
     */
    @NonNull
    public List<Expense> getExpensesForDay(@NonNull Date date)
    {
        date = DateHelper.cleanDate(date);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_DATE + " = " + date.getTime(), null, null, null, null, null);
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
     * Get a sum of all amount of expenses until the given day
     *
     * @param day
     * @return
     */
    public int getBalanceForDay(@NonNull Date day)
    {
        day = DateHelper.cleanDate(day);

        Cursor cursor = null;
        try
        {
            cursor = database.rawQuery("SELECT SUM(" + SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT + ") FROM " + SQLiteDBHelper.TABLE_EXPENSE + " WHERE " + SQLiteDBHelper.COLUMN_EXPENSE_DATE + " <= " + day.getTime(), null);

            if(cursor.moveToFirst())
            {
                return cursor.getInt(0);
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
        return database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_DB_ID+"="+expense.getId(), null) > 0;
    }

    /**
     * Delete all expense for this monthly expense
     *
     * @param monthlyExpense
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForMonthlyExpense(@NonNull MonthlyExpense monthlyExpense)
    {
        return database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId(), null) > 0;
    }

    /**
     * Delete all expense for this monthly expense from the given date (included)
     *
     * @param monthlyExpense
     * @param fromDate
     * @return true on success, false on error
     */
    public boolean deleteAllExpenseForMonthlyExpenseFromDate(@NonNull MonthlyExpense monthlyExpense, @NonNull Date fromDate)
    {
        return database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+">="+fromDate.getTime(), null) > 0;
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
        return database.delete(SQLiteDBHelper.TABLE_EXPENSE, SQLiteDBHelper.COLUMN_EXPENSE_MONTHLY_ID+"="+monthlyExpense.getId()+" AND "+SQLiteDBHelper.COLUMN_EXPENSE_DATE+"<"+toDate.getTime(), null) > 0;
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
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT)),
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

        values.put(SQLiteDBHelper.COLUMN_EXPENSE_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_EXPENSE_DATE, expense.getDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_EXPENSE_AMOUNT, expense.getAmount());

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
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT)),
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

        values.put(SQLiteDBHelper.COLUMN_MONTHLY_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_RECURRING_DATE, expense.getRecurringDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT, expense.getAmount());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_MODIFIED, expense.isModified() ? 1 : 0);

        return values;
    }
}
