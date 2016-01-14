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
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.DateHelper;

import java.util.Date;
import java.util.List;

/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
public class MonthlyReportActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.monthly_report_progress_bar);
        final ViewPager pager = (ViewPager) findViewById(R.id.monthly_report_view_pager);
        final View content = findViewById(R.id.monthly_report_content);

        // Load list of monthly asynchronously since it can take time
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

                loadViewPager(pager, dates);

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

    private void loadViewPager(final ViewPager pager, final List<Date> dates)
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
        pager.setCurrentItem(dates.size() - 1, false);
    }

}
