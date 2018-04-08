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

package com.ajapplications.budgeteerbuddy.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ajapplications.budgeteerbuddy.model.db.DB;

/**
 * An {@link AppCompatActivity} that contains an opened DB connection to perform queries
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
