package com.benoitletondor.easybudget.view;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.benoitletondor.easybudget.model.db.DB;

/**
 * An ActionBarActivity that contains an open connection DB to perform queries
 *
 * @author Benoit LETONDOR
 */
public abstract class DBActivity extends ActionBarActivity
{
    /**
     * An open DB connection that can be used
     */
    protected DB db;

// ------------------------------------------>

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        db = new DB(getApplicationContext());
    }

    @Override
    protected void onDestroy()
    {
        db.close();

        super.onDestroy();
    }
}
