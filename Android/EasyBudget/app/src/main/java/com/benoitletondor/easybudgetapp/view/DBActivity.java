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

package com.benoitletondor.easybudgetapp.view;

import androidx.appcompat.app.AppCompatActivity;

import com.benoitletondor.easybudgetapp.model.db.DB;

import static org.koin.java.KoinJavaComponent.get;

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
    protected DB db = get(DB.class);

// ------------------------------------------>

    @Override
    protected void onDestroy()
    {
        db.close();

        super.onDestroy();
    }
}
