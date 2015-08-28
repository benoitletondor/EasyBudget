package com.benoitletondor.easybudget.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
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
    public final String PAGER_NEXT_INTENT     = "welcome.pager.next";
    /**
     * Intent broadcasted by pager fragments to go previous
     */
    public final String PAGER_PREVIOUS_INTENT = "welcome.pager.previous";

// ------------------------------------------>

    /**
     * The view pager
     */
    private ViewPager pager;

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
    }

    @Override
    public void onBackPressed()
    {
        // Prevent back to exit
    }
}
