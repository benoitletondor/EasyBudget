package com.benoitletondor.easybudget.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.view.welcome.Onboarding1Fragment;
import com.benoitletondor.easybudget.view.welcome.Onboarding2Fragment;

/**
 * Welcome screen activity
 *
 * @author Benoit LETONDOR
 */
public class WelcomeActivity extends AppCompatActivity
{
    /**
     * Value used for the {@link com.benoitletondor.easybudget.helper.ParameterKeys#ONBOARDING_STEP} when completed
     */
    public final static int STEP_COMPLETED = Integer.MAX_VALUE;

    /**
     * Intent broadcasted by pager fragments to go next
     */
    public final static String PAGER_NEXT_INTENT     = "welcome.pager.next";
    /**
     * Intent broadcasted by pager fragments to go previous
     */
    public final static String PAGER_PREVIOUS_INTENT = "welcome.pager.previous";

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
                }

                return null;
            }

            @Override
            public int getCount()
            {
                return 2;
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(PAGER_NEXT_INTENT);
        filter.addAction(PAGER_PREVIOUS_INTENT);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( PAGER_NEXT_INTENT.equals(intent.getAction()) )
                {
                    if( pager.getCurrentItem() < pager.getAdapter().getCount() )
                    {
                        pager.setCurrentItem(pager.getCurrentItem()+1, true);
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
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
        }

        // Prevent back to leave activiy
    }
}
