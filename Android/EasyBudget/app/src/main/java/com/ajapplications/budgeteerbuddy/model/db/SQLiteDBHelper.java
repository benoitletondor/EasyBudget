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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.ajapplications.budgeteerbuddy.model.RecurringExpenseType;

/**
 * @author Benoit LETONDOR
 */
public final class SQLiteDBHelper extends SQLiteOpenHelper
{
    protected static final String TABLE_EXPENSE                 = "expense";
    protected static final String COLUMN_EXPENSE_DB_ID          = "_expense_id";
    protected static final String COLUMN_EXPENSE_TITLE          = "title";
    protected static final String COLUMN_EXPENSE_AMOUNT         = "amount";
    protected static final String COLUMN_EXPENSE_DATE           = "date";
    protected static final String COLUMN_EXPENSE_RECURRING_ID   = "monthly_id";

    protected static final String TABLE_RECURRING_EXPENSE           = "monthlyexpense";
    protected static final String COLUMN_RECURRING_DB_ID            = "_expense_id";
    protected static final String COLUMN_RECURRING_TITLE            = "title";
    protected static final String COLUMN_RECURRING_AMOUNT           = "amount";
    protected static final String COLUMN_RECURRING_RECURRING_DATE   = "recurringDate";
    protected static final String COLUMN_RECURRING_MODIFIED         = "modified";
    protected static final String COLUMN_RECURRING_TYPE             = "type";

// -------------------------------------------->

    private static final String DATABASE_NAME    = "easybudget.db";
    private static final int    DATABASE_VERSION = 3;

// -------------------------------------------->

    public SQLiteDBHelper(@NonNull Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database)
    {
        database.execSQL("create table "
            + TABLE_EXPENSE + "("
            + COLUMN_EXPENSE_DB_ID + " integer primary key autoincrement, "
            + COLUMN_EXPENSE_TITLE + " text not null, "
            + COLUMN_EXPENSE_AMOUNT + " integer not null, "
            + COLUMN_EXPENSE_DATE + " integer not null, "
            + COLUMN_EXPENSE_RECURRING_ID + " integer null );");

        database.execSQL("CREATE INDEX D_i on "+ TABLE_EXPENSE +"("+ COLUMN_EXPENSE_DATE +");");

        database.execSQL("create table "
            + TABLE_RECURRING_EXPENSE + "("
            + COLUMN_RECURRING_DB_ID + " integer primary key autoincrement, "
            + COLUMN_RECURRING_TITLE + " text not null, "
            + COLUMN_RECURRING_AMOUNT + " integer not null, "
            + COLUMN_RECURRING_MODIFIED + " integer not null, "
            + COLUMN_RECURRING_RECURRING_DATE + " integer not null, "
            + COLUMN_RECURRING_TYPE + " text not null DEFAULT '"+RecurringExpenseType.MONTHLY+"');");
    }

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{
        if( oldVersion<2 )
        {
            database.execSQL("UPDATE "+TABLE_EXPENSE+" SET "+COLUMN_EXPENSE_AMOUNT+" = "+COLUMN_EXPENSE_AMOUNT+" * 100");
            database.execSQL("UPDATE "+ TABLE_RECURRING_EXPENSE +" SET "+ COLUMN_RECURRING_AMOUNT +" = "+ COLUMN_RECURRING_AMOUNT +" * 100");
        }

        if( oldVersion < 3 )
        {
            database.execSQL("ALTER TABLE "+TABLE_RECURRING_EXPENSE+" ADD COLUMN "+COLUMN_RECURRING_TYPE+" text not null DEFAULT '"+RecurringExpenseType.MONTHLY+"'");
        }
	}
}