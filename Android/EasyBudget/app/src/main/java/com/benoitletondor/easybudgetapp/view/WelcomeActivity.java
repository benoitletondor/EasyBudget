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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.ViewAnimationUtils;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.model.db.DB;
import com.benoitletondor.easybudgetapp.view.welcome.Onboarding1Fragment;
import com.benoitletondor.easybudgetapp.view.welcome.Onboarding2Fragment;
import com.benoitletondor.easybudgetapp.view.welcome.Onboarding3Fragment;
import com.benoitletondor.easybudgetapp.view.welcome.Onboarding4Fragment;
import com.benoitletondor.easybudgetapp.view.welcome.OnboardingFragment;

import me.relex.circleindicator.CircleIndicator;

/**
 * Welcome screen activity
 *
 * @author Benoit LETONDOR
 */
public class WelcomeActivity extends DBActivity
{
    /**
     * Value used for the {@link com.benoitletondor.easybudgetapp.helper.ParameterKeys#ONBOARDING_STEP} when completed
     */
    public final static int STEP_COMPLETED = Integer.MAX_VALUE;

    public final static String ANIMATE_TRANSITION_KEY = "animate";
    public final static String CENTER_X_KEY           = "centerX";
    public final static String CENTER_Y_KEY           = "centerY";

    /**
     * Intent broadcasted by pager fragments to go next
     */
    public final static String PAGER_NEXT_INTENT     = "welcome.pager.next";
    /**
     * Intent broadcasted by pager fragments to go previous
     */
    public final static String PAGER_PREVIOUS_INTENT = "welcome.pager.previous";
    /**
     * Intent broadcasted by pager fragments when welcome onboarding is done
     */
    public final static String PAGER_DONE_INTENT     = "welcome.pager.done";

// ------------------------------------------>

    /**
     * The view pager
     */
    private ViewPager         pager;
    /**
     * Broadcast receiver for intent sent by fragments
     */
    private BroadcastReceiver receiver;

// ------------------------------------------>

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Reinit step to 0 if already completed
        if (getStep() == STEP_COMPLETED)
        {
            setStep(0);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        pager = (ViewPager) findViewById(R.id.welcome_view_pager);
        pager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager())
        {
            @Override
            public Fragment getItem(int position)
            {
                switch (position)
                {
                    case 0:
                        return new Onboarding1Fragment();
                    case 1:
                        return new Onboarding2Fragment();
                    case 2:
                        return new Onboarding3Fragment();
                    case 3:
                        return new Onboarding4Fragment();
                }

                return null;
            }

            @Override
            public int getCount()
            {
                return 4;
            }
        });
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                OnboardingFragment fragment = (OnboardingFragment) ((FragmentStatePagerAdapter) pager.getAdapter()).getItem(position);
                UIHelper.setStatusBarColor(WelcomeActivity.this, fragment.getStatusBarColor());

                setStep(position);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        pager.setOffscreenPageLimit(pager.getAdapter().getCount()); // preload all fragments for transitions smoothness

        // Circle indicator
        ((CircleIndicator) findViewById(R.id.welcome_view_pager_indicator)).setViewPager(pager);

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
                    setStep(STEP_COMPLETED);
                    WelcomeActivity.this.setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        int initialStep = getStep();

        // Init pager at the current step
        pager.setCurrentItem(initialStep, false);

        // Set status bar color
        OnboardingFragment fragment = (OnboardingFragment) ((FragmentStatePagerAdapter) pager.getAdapter()).getItem(initialStep);
        UIHelper.setStatusBarColor(this, fragment.getStatusBarColor());
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

        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /**
     * Method that a child (fragment) can call to get the DB connexion
     *
     * @return the db connexion
     */
    @NonNull
    public DB getDB()
    {
        return db;
    }

// ------------------------------------>

    /**
     * Get the current saved onboarding step
     *
     * @return
     */
    private int getStep()
    {
        return Parameters.getInstance(this).getInt(ParameterKeys.ONBOARDING_STEP, 0);
    }

    /**
     * Save the given step as onboarding step
     *
     * @param step
     */
    private void setStep(int step)
    {
        Parameters.getInstance(this).putInt(ParameterKeys.ONBOARDING_STEP, step);
    }
}
