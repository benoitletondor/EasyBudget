package com.benoitletondor.easybudget.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author Benoit LETONDOR
 */
public final class SQLiteDBHelper extends SQLiteOpenHelper
{
    protected static final String TABLE_ONE_TIME_EXPENSE = "onetimeexpense";
    protected static final String COLUMN_ONE_TIME_DB_ID  = "_expense_id";
    protected static final String COLUMN_ONE_TIME_AMOUNT = "amount";
    protected static final String COLUMN_ONE_TIME_DATE   = "date";

    protected static final String TABLE_MONTHLY_EXPENSE         = "monthlyexpense";
    protected static final String COLUMN_MONTHLY_DB_ID          = "_expense_id";
    protected static final String COLUMN_MONTHLY_AMOUNT         = "amount";
    protected static final String COLUMN_MONTHLY_STARTDATE      = "startDate";
    protected static final String COLUMN_MONTHLY_ENDDATE        = "endDate";
    protected static final String COLUMN_MONTHLY_MODIFICATIONS  = "modifications";

// -------------------------------------------->

    private static final String DATABASE_NAME   = "easybudget.db";
    private static final int DATABASE_VERSION   = 1;

// -------------------------------------------->

    public SQLiteDBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database)
    {
        database.execSQL("create table "
                + TABLE_ONE_TIME_EXPENSE + "("
				+ COLUMN_ONE_TIME_DB_ID + " integer primary key autoincrement, "
                + COLUMN_ONE_TIME_AMOUNT + " integer not null, "
				+ COLUMN_ONE_TIME_DATE + " integer not null );");

        database.execSQL("create table "
                + TABLE_MONTHLY_EXPENSE + "("
                + COLUMN_MONTHLY_DB_ID + " integer primary key autoincrement, "
                + COLUMN_MONTHLY_AMOUNT + " integer not null, "
                + COLUMN_MONTHLY_ENDDATE + " integer not null, "
                + COLUMN_MONTHLY_MODIFICATIONS + " text not null, "
                + COLUMN_MONTHLY_STARTDATE + " integer not null );");
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{

	}
}