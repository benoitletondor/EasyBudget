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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.benoitletondor.easybudgetapp.EasyBudget;
import com.benoitletondor.easybudgetapp.R;
import com.google.android.gms.appinvite.AppInviteInvitation;

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
     * Request code used by app invite
     */
    protected static final int APP_INVITE_REQUEST = 1001;
    /**
     * Request code used by premium activity
     */
    protected static final int PREMIUM_ACTIVITY = 20020;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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

        // iab management
        if( ((EasyBudget) getApplication()).handleActivityResult(requestCode, resultCode, data) )
        {
            return;
        }

        if (requestCode == APP_INVITE_REQUEST)
        {
            if (resultCode == RESULT_OK)
            {
                // Check how many invitations were sent and log a message
                // The ids array contains the unique invitation ids for each invitation sent
                // (one for each contact select by the user). You can use these for analytics
                // as the ID will be consistent on the sending and receiving devices.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);

                ((EasyBudget) getApplication()).trackNumberOfInvitsSent(ids.length);
            }
        }
        else if( requestCode == PREMIUM_ACTIVITY )
        {
            if( resultCode == Activity.RESULT_OK )
            {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(USER_GONE_PREMIUM_INTENT));
            }
        }
    }

}
