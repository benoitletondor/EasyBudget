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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.benoitletondor.easybudgetapp.R;

import java.util.Objects;

/**
 * Activity that displays settings using the {@link PreferencesFragment}
 *
 * @author Benoit LETONDOR
 */
public class SettingsActivity extends AppCompatActivity
{
    /**
     * Key to specify that the premium popup should be shown to the user
     */
    public static final String SHOW_PREMIUM_INTENT_KEY = "showPremium";
    /**
     * Intent action broadcast when the user has successfully completed the {@link PremiumActivity}
     */
    public static final String USER_GONE_PREMIUM_INTENT = "user.ispremium";
    /**
     * Request code used by premium activity
     */
    protected static final int PREMIUM_ACTIVITY = 20020;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        if( id == android.R.id.home ) // Back button of the actionbar
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == PREMIUM_ACTIVITY )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(USER_GONE_PREMIUM_INTENT));
            }
        }
    }

}
