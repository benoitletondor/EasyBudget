package com.benoitletondor.easybudget.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

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
    private SQLiteDatabase database;
    /**
     * The DB Helper
     */
    private SQLiteDBHelper databaseHelper;

// -------------------------------------------->

    /**
     * Create and open a new DB
     *
     * @param context
     * @throws SQLiteException
     */
    public DB(Context context) throws SQLiteException
    {
        if (context == null)
        {
            throw new NullPointerException("context==null");
        }

		databaseHelper = new SQLiteDBHelper(context.getApplicationContext());
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
            databaseHelper = null;
        }
        catch (Exception e)
        {
            Logger.error("Error while closing SQLite DB", e);
        }
    }

    /**
     * Clear all DB content (<b>for test purpose</b>)
     */
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
     * @return
     */
    public long addExpense(Expense expense)
    {
        if( expense == null )
        {
            throw new NullPointerException("expense==null");
        }

        return database.insert(SQLiteDBHelper.TABLE_EXPENSE, null, generateContentValuesForExpense(expense));
    }

    /**
     * Get all one time expense for a day
     *
     * @param date
     * @return
     */
    public List<Expense> getOneTimeExpensesForDay(Date date)
    {
        date = DateHelper.cleanDate(date);

        Cursor cursor = null;
        try
        {
            List<Expense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_EXPENSE, null, SQLiteDBHelper.COLUMN_EXPENSE_DATE + " = "+date.getTime(), null, null, null, null, null);
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
     * Add a monthly expense
     *
     * @param expense
     * @return ID of the inserted monthly expense on success, exception or -1 on error
     */
    public long addMonthlyExpense(MonthlyExpense expense)
    {
        if( expense == null )
        {
            throw new NullPointerException("expense==null");
        }

        return database.insert(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, generateContentValuesForMonthlyExpense(expense));
    }

// -------------------------------------------->

    /**
     * Deserialize an expense from DB
     *
     * @param cursor
     * @return
     */
    private static Expense ExpenseFromCursor(Cursor cursor)
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
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_EXPENSE_DATE))),
            monthlyId > 0 ? monthlyId : null
        );
    }

    /**
     * Generate serialized values for an expense
     *
     * @param expense
     * @return
     */
    private static ContentValues generateContentValuesForExpense(Expense expense)
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
    private static MonthlyExpense MonthlyExpenseFromCursor(Cursor cursor)
    {
        return new MonthlyExpense
        (
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
    private static ContentValues generateContentValuesForMonthlyExpense(MonthlyExpense expense)
    {
        final ContentValues values = new ContentValues();

        values.put(SQLiteDBHelper.COLUMN_MONTHLY_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_RECURRING_DATE, expense.getRecurringDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT, expense.getAmount());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_MODIFIED, expense.isModified() ? 1 : 0);

        return values;
    }
}
