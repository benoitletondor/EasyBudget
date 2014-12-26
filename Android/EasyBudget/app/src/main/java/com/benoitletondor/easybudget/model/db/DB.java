package com.benoitletondor.easybudget.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.benoitletondor.easybudget.helper.DateHelper;
import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.model.MonthlyExpense;
import com.benoitletondor.easybudget.model.OneTimeExpense;

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
        database.delete(SQLiteDBHelper.TABLE_ONE_TIME_EXPENSE, null, null);
        database.delete(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, null);
    }

// -------------------------------------------->

    /**
     * Add a one time expense into DB
     *
     * @param expense
     */
    public void addOneTimeExpense(OneTimeExpense expense)
    {
        if( expense == null )
        {
            throw new NullPointerException("expense==null");
        }

        database.insert(SQLiteDBHelper.TABLE_ONE_TIME_EXPENSE, null, generateContentValuesForOneTimeExpense(expense));
    }

    /**
     * Get all one time expense for a day
     *
     * @param date
     * @return
     */
    public List<OneTimeExpense> getOneTimeExpensesForDay(Date date)
    {
        date = DateHelper.cleanDate(date);

        Cursor cursor = null;
        try
        {
            List<OneTimeExpense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_ONE_TIME_EXPENSE, null, SQLiteDBHelper.COLUMN_ONE_TIME_DATE + " = "+date.getTime(), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                expenses.add(OneTimeExpenseFromCursor(cursor));
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
     */
    public void addMonthlyExpense(MonthlyExpense expense)
    {
        if( expense == null )
        {
            throw new NullPointerException("expense==null");
        }

        try
        {
            database.insert(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, generateContentValuesForMonthlyExpense(expense));
        }
        catch(Exception e)
        {
            throw new RuntimeException("Error while serializing Monthly expense to SQLite", e);
        }
    }

    /**
     * Get all monthly expense for a day
     *
     * @param date
     * @return
     */
    public List<MonthlyExpense> getMonthyExpensesForDay(Date date)
    {
        date = DateHelper.cleanDate(date);

        Cursor cursor = null;
        try
        {
            List<MonthlyExpense> expenses = new ArrayList<>();

            cursor = database.query(SQLiteDBHelper.TABLE_MONTHLY_EXPENSE, null, SQLiteDBHelper.COLUMN_MONTHLY_DAYOFMONTH + " = "+DateHelper.getDayOfMonth(date), null, null, null, null, null);
            while( cursor.moveToNext() )
            {
                try
                {
                    expenses.add(MonthlyExpenseFromCursor(cursor));
                }
                catch(Exception e)
                {
                    throw new RuntimeException("Error while deserializing MonthlyExpense", e);
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

// -------------------------------------------->

    /**
     * Deserialize a one time expense from DB
     *
     * @param cursor
     * @return
     */
    private static OneTimeExpense OneTimeExpenseFromCursor(Cursor cursor)
    {
        return new OneTimeExpense
        (
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_ONE_TIME_TITLE)),
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_ONE_TIME_AMOUNT)),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_ONE_TIME_DATE)))
        );
    }

    /**
     * Generate serialized values for a one time expense
     *
     * @param expense
     * @return
     */
    private static ContentValues generateContentValuesForOneTimeExpense(OneTimeExpense expense)
    {
        final ContentValues values = new ContentValues();

        values.put(SQLiteDBHelper.COLUMN_ONE_TIME_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_ONE_TIME_DATE, expense.getDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_ONE_TIME_AMOUNT, expense.getAmount());

        return values;
    }

    /**
     * Deserialize a monthly expense from DB
     *
     * @param cursor
     * @return
     * @throws JSONException
     */
    private static MonthlyExpense MonthlyExpenseFromCursor(Cursor cursor) throws JSONException
    {
        return new MonthlyExpense
        (
            cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_TITLE)),
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_START_AMOUNT)),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_STARTDATE))),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_ENDDATE))),
            MonthlyExpense.jsonToModifications(cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_MODIFICATIONS)))
        );
    }

    /**
     * Generate serialized values for a monthly expense
     *
     * @param expense
     * @return
     * @throws JSONException
     */
    private static ContentValues generateContentValuesForMonthlyExpense(MonthlyExpense expense) throws JSONException
    {
        final ContentValues values = new ContentValues();

        values.put(SQLiteDBHelper.COLUMN_MONTHLY_TITLE, expense.getTitle());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_STARTDATE, expense.getStartDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_ENDDATE, expense.getEndDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_START_AMOUNT, expense.getStartAmount());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_MODIFICATIONS, MonthlyExpense.modificationsToJson(expense));
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_DAYOFMONTH, expense.getDayOfMonth());

        return values;
    }
}
