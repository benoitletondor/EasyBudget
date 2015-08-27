package com.benoitletondor.easybudget.view;

import android.os.Bundle;

import com.benoitletondor.easybudget.model.db.DB;

/**
 * A {@link BatchActivity} that contains an opened DB connection to perform queries
 *
 * @author Benoit LETONDOR
 */
public abstract class DBActivity extends BatchActivity
{
    /**
     * An opened DB connection that is ready to be used
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
