/*
 *   Copyright 2016 Benoit LETONDOR
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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.DateHelper;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportFragment;

import java.util.Date;
import java.util.List;

/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener
{
    /**
     * Extra to add the the launch intent to specify that user comes from the notification (used to
     * show not the current month but the last one)
     */
    public static final String FROM_NOTIFICATION_EXTRA = "fromNotif";

    /**
     * List of first date of each month available
     */
    private List<Date> dates;
    /**
     * TextView that displays the name of the month
     */
    private TextView monthTitleTv;
    /**
     * Button to go the previous month
     */
    private Button previousMonthButton;
    /**
     * Button to go the next month
     */
    private Button nextMonthButton;
    /**
     * ViewPager used to display each month in a Fragment
     */
    private ViewPager pager;
    /**
     * The current {@link #pager} position
     */
    private int selectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.monthly_report_progress_bar);
        final View content = findViewById(R.id.monthly_report_content);
        monthTitleTv = (TextView) findViewById(R.id.monthly_report_month_title_tv);
        previousMonthButton = (Button) findViewById(R.id.monthly_report_previous_month_button);
        nextMonthButton = (Button) findViewById(R.id.monthly_report_next_month_button);
        pager = (ViewPager) findViewById(R.id.monthly_report_view_pager);

        previousMonthButton.setText("<");
        nextMonthButton.setText(">");

        previousMonthButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if( selectedPosition > 0 )
                {
                    selectPagerItem(selectedPosition - 1, true);
                }
            }
        });

        nextMonthButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if( selectedPosition < dates.size() - 1 )
                {
                    selectPagerItem(selectedPosition + 1, true);
                }
            }
        });

        UIHelper.removeButtonBorder(previousMonthButton);
        UIHelper.removeButtonBorder(nextMonthButton);

        // Load list of months asynchronously since it can take time
        new AsyncTask<Void, Void, List<Date>>()
        {
            @Override
            protected List<Date> doInBackground(Void... params)
            {
                return DateHelper.getListOfMonthsAvailableForUser(MonthlyReportActivity.this);
            }

            @Override
            protected void onPostExecute(List<Date> dates)
            {
                if( isFinishing() )
                {
                    return;
                }

                MonthlyReportActivity.this.dates = dates;

                configureViewPager();

                progressBar.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if( id == android.R.id.home ) // Back button of the actionbar
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Configure the {@link #pager} adapter and listener.
     */
    private void configureViewPager()
    {
        pager.setOffscreenPageLimit(0);
        pager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager())
        {
            @Override
            public Fragment getItem(int position)
            {
                return new MonthlyReportFragment(dates.get(position));
            }

            @Override
            public int getCount()
            {
                return dates.size();
            }
        });
        pager.addOnPageChangeListener(this);

        // Show previous month if user comes from the notification
        if( getIntent().getBooleanExtra(FROM_NOTIFICATION_EXTRA, false) && dates.size() > 1 )
        {
            selectPagerItem(dates.size() - 2, false);
        }
        else
        {
            selectPagerItem(dates.size() - 1, false);
        }
    }

    /**
     * Set the given item to the pager and trigger the {@link ViewPager.OnPageChangeListener}
     * callback.
     *
     * @param position item of the pager
     * @param animate should the pager animate the transition
     */
    private void selectPagerItem(int position, boolean animate)
    {
        pager.setCurrentItem(position, animate);
        onPageSelected(position);
    }

// ------------------------------------------>

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {

    }

    @Override
    public void onPageSelected(int position)
    {
        selectedPosition = position;

        Date date = dates.get(position);

        monthTitleTv.setText(DateHelper.getMonthTitle(this, date));

        // Last and first available month
        boolean last = position == dates.size() - 1;
        boolean first = position == 0;

        nextMonthButton.setEnabled(!last);
        nextMonthButton.setTextColor(ContextCompat.getColor(this, last ? R.color.monthly_report_disabled_month_button : android.R.color.white));
        previousMonthButton.setEnabled(!first);
        previousMonthButton.setTextColor(ContextCompat.getColor(this, first ? R.color.monthly_report_disabled_month_button : android.R.color.white));
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {

    }
}
