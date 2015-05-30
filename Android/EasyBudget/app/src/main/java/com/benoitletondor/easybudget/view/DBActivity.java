package com.benoitletondor.easybudget.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.benoitletondor.easybudget.model.db.DB;

/**
 * An AppCompatActivity that contains an opened DB connection to perform queries
 *
 * @author Benoit LETONDOR
 */
public abstract class DBActivity extends AppCompatActivity
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
