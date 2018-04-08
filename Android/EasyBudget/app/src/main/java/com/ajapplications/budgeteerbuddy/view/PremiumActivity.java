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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ajapplications.budgeteerbuddy.EasyBudget;
import com.ajapplications.budgeteerbuddy.PremiumPurchaseListener;
import com.ajapplications.budgeteerbuddy.view.premium.Premium1Fragment;
import com.ajapplications.budgeteerbuddy.view.premium.Premium2Fragment;
import com.ajapplications.budgeteerbuddy.view.premium.Premium3Fragment;
import com.ajapplications.budgeteerbuddy.R;

import me.relex.circleindicator.CircleIndicator;

/**
 * Activity that contains the premium onboarding screen. This activity should return with a
 * {@link Activity#RESULT_OK} if user has successfully purchased premium.
 *
 * @author Benoit LETONDOR
 */
public class PremiumActivity extends AppCompatActivity
{
    /**
     * The view pager
     */
    private ViewPager pager;

// ------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        // Cancelled by default
        setResult(Activity.RESULT_CANCELED);

        pager = (ViewPager) findViewById(R.id.premium_view_pager);
        pager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager())
        {
            @Override
            public Fragment getItem(int position)
            {
                switch (position)
                {
                    case 0:
                        return new Premium1Fragment();
                    case 1:
                        return new Premium2Fragment();
                    case 2:
                        return new Premium3Fragment();
                }

                return null;
            }

            @Override
            public int getCount()
            {
                return 3;
            }
        });
        pager.setOffscreenPageLimit(pager.getAdapter().getCount()); // preload all fragments for transitions smoothness

        // Circle indicator
        ((CircleIndicator) findViewById(R.id.premium_view_pager_indicator)).setViewPager(pager);

        findViewById(R.id.premium_not_now_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        findViewById(R.id.premium_cta_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Show loader
                final ProgressDialog loading = ProgressDialog.show(PremiumActivity.this,
                        getResources().getString(R.string.iab_purchase_wait_title),
                        getResources().getString(R.string.iab_purchase_wait_message),
                        true, false);

                ((EasyBudget) getApplication()).launchPremiumPurchaseFlow(PremiumActivity.this, new PremiumPurchaseListener()
                {
                    @Override
                    public void onUserCancelled()
                    {
                        loading.dismiss();
                    }

                    @Override
                    public void onPurchaseError(String error)
                    {
                        loading.dismiss();

                        new AlertDialog.Builder(PremiumActivity.this)
                            .setTitle(R.string.iab_purchase_error_title)
                            .setMessage(getResources().getString(R.string.iab_purchase_error_message, error))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    }

                    @Override
                    public void onPurchaseSuccess()
                    {
                        loading.dismiss();
                        setResult(Activity.RESULT_OK); // Important to update the UI
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        if( pager.getCurrentItem() > 0 )
        {
            pager.setCurrentItem(pager.getCurrentItem()-1, true);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // iab management
        if( !((EasyBudget) getApplication()).handleActivityResult(requestCode, resultCode, data) )
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
