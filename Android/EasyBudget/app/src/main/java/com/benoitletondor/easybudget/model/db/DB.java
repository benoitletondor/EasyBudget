package com.benoitletondor.easybudget.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.model.MonthlyExpense;
import com.benoitletondor.easybudget.model.OneTimeExpense;

import org.json.JSONException;

import java.util.Date;

/**
 * @author Benoit LETONDOR
 */
public final class DB
{
    /**
     * Saved app context
     */
    private Context        context;
    /**
     * The SQLLite DB
     */
    private SQLiteDatabase database;
    /**
     * The DB Helper
     */
    private SQLiteDBHelper databaseHelper;

// -------------------------------------------->

    public DB(Context context) throws SQLiteException
    {
        if (context == null)
        {
            throw new NullPointerException("context==null");
        }

        this.context = context.getApplicationContext();
		databaseHelper = new SQLiteDBHelper(this.context);
		database = databaseHelper.getWritableDatabase();
	}

// -------------------------------------------->



// -------------------------------------------->

    private static OneTimeExpense OneTimeExpenseFromCursor(Cursor cursor)
    {
        return new OneTimeExpense
        (
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_ONE_TIME_AMOUNT)),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_ONE_TIME_DATE)))
        );
    }

    private static ContentValues generateContentValuesForOneTimeExpense(OneTimeExpense expense)
    {
        final ContentValues values = new ContentValues();

        values.put(SQLiteDBHelper.COLUMN_ONE_TIME_DATE, expense.getDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_ONE_TIME_AMOUNT, expense.getAmount());

        return values;
    }

    private static MonthlyExpense MonthlyExpenseFromCursor(Cursor cursor) throws JSONException
    {
        return new MonthlyExpense
        (
            cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT)),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_STARTDATE))),
            new Date(cursor.getInt(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_ENDDATE))),
            MonthlyExpense.jsonToModifications(cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_MONTHLY_MODIFICATIONS)))
        );
    }

    private static ContentValues generateContentValuesForMonthlyExpense(MonthlyExpense expense) throws JSONException
    {
        final ContentValues values = new ContentValues();

        values.put(SQLiteDBHelper.COLUMN_MONTHLY_STARTDATE, expense.getStartDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_ENDDATE, expense.getEndDate().getTime());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_AMOUNT, expense.getAmount());
        values.put(SQLiteDBHelper.COLUMN_MONTHLY_MODIFICATIONS, MonthlyExpense.modificationsToJson(expense));

        return values;
    }
}
