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

import android.animation.Animator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;

import com.benoitletondor.easybudgetapp.EasyBudget;
import com.benoitletondor.easybudgetapp.PremiumPurchaseListener;
import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.view.premium.Premium1Fragment;
import com.benoitletondor.easybudgetapp.view.premium.Premium2Fragment;

import me.relex.circleindicator.CircleIndicator;

public class PremiumActivity extends AppCompatActivity
{
    /**
     * Intent broadcast by pager fragments to go next
     */
    public final static String PAGER_NEXT_INTENT     = "premium.pager.next";
    /**
     * Intent broadcast by pager fragments to go previous
     */
    public final static String PAGER_PREVIOUS_INTENT = "premium.pager.previous";
    /**
     * Intent broadcast by pager fragments when premium onboarding is done
     */
    public final static String PAGER_DONE_INTENT     = "premium.pager.done";

    public final static String ANIMATE_TRANSITION_KEY = "animate";
    public final static String CENTER_X_KEY           = "centerX";
    public final static String CENTER_Y_KEY           = "centerY";

// ------------------------------------->

    /**
     * The view pager
     */
    private ViewPager pager;
    /**
     * Broadcast receiver for intent sent by fragments
     */
    private BroadcastReceiver receiver;

// ------------------------------------->

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        setResult(Activity.RESULT_CANCELED);

        pager = (ViewPager) findViewById(R.id.premium_view_pager);
        pager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager())
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
                }

                return null;
            }

            @Override
            public int getCount()
            {
                return 2;
            }
        });
        pager.setOffscreenPageLimit(pager.getAdapter().getCount()); // preload all fragments for transitions smoothness

        // Circle indicator
        ((CircleIndicator) findViewById(R.id.premium_view_pager_indicator)).setViewPager(pager);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PAGER_NEXT_INTENT);
        filter.addAction(PAGER_PREVIOUS_INTENT);
        filter.addAction(PAGER_DONE_INTENT);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( PAGER_NEXT_INTENT.equals(intent.getAction()) && pager.getCurrentItem() < pager.getAdapter().getCount()-1 )
                {
                    if( intent.getBooleanExtra(ANIMATE_TRANSITION_KEY, false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
                    {
                        // get the center for the clipping circle
                        int cx = intent.getIntExtra(CENTER_X_KEY, (int) pager.getX() + pager.getWidth() / 2);
                        int cy = intent.getIntExtra(CENTER_Y_KEY, (int) pager.getY() + pager.getHeight() / 2);

                        // get the final radius for the clipping circle
                        int finalRadius = Math.max(pager.getWidth(), pager.getHeight());

                        // create the animator for this view (the start radius is zero)
                        Animator anim = ViewAnimationUtils.createCircularReveal(pager, cx, cy, 0, finalRadius);

                        // make the view visible and start the animation
                        pager.setCurrentItem(pager.getCurrentItem() + 1, false);
                        anim.start();
                    }
                    else
                    {
                        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
                    }
                }
                else if( PAGER_PREVIOUS_INTENT.equals(intent.getAction()) && pager.getCurrentItem() > 0 )
                {
                    pager.setCurrentItem(pager.getCurrentItem()-1, true);
                }
                else if( PAGER_DONE_INTENT.equals(intent.getAction()) )
                {
                    finish();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

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
                        setResult(Activity.RESULT_OK);
                        finish();

                        new AlertDialog.Builder(PremiumActivity.this)
                            .setTitle(R.string.iab_purchase_success_title)
                            .setMessage(R.string.iab_purchase_success_message)
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
                });
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if( pager.getCurrentItem() > 0 )
        {
            pager.setCurrentItem(pager.getCurrentItem()-1, true);
            return;
        }

        finish();
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
