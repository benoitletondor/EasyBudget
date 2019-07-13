/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.report.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.getMonthTitle
import com.benoitletondor.easybudgetapp.helper.removeButtonBorder
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportFragment
import kotlinx.android.synthetic.main.activity_monthly_report.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportBaseActivity : AppCompatActivity(), ViewPager.OnPageChangeListener {

    private val viewModel: MonthlyReportBaseViewModel by viewModel()

    private var ignoreNextPageSelectedEvent: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_report)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if( savedInstanceState == null ) {
            viewModel.loadData(intent.getBooleanExtra(FROM_NOTIFICATION_EXTRA, false))
        }

        monthly_report_previous_month_button.text = "<"
        monthly_report_next_month_button.text = ">"

        monthly_report_previous_month_button.setOnClickListener {
            viewModel.onPreviousMonthButtonClicked()
        }

        monthly_report_next_month_button.setOnClickListener {
            viewModel.onNextMonthButtonClicked()
        }

        monthly_report_previous_month_button.removeButtonBorder()
        monthly_report_next_month_button.removeButtonBorder()

        viewModel.datesLiveData.observe(this, Observer { dates ->
            configureViewPager(dates)

            monthly_report_progress_bar.visibility = View.GONE
            monthly_report_content.visibility = View.VISIBLE
        })

        viewModel.selectedPositionLiveData.observe(this, Observer { (position, date, isLatestMonth) ->
            if( !ignoreNextPageSelectedEvent ) {
                monthly_report_view_pager.setCurrentItem(position, true)
            }

            ignoreNextPageSelectedEvent = false

            monthly_report_month_title_tv.text = date.getMonthTitle(this)

            // Last and first available month
            val isFirstMonth = position == 0

            monthly_report_next_month_button.isEnabled = !isLatestMonth
            monthly_report_previous_month_button.isEnabled = !isFirstMonth
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<Date>) {
        monthly_report_view_pager.offscreenPageLimit = 0
        monthly_report_view_pager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return MonthlyReportFragment.newInstance(dates[position])
            }

            override fun getCount(): Int {
                return dates.size
            }
        }
        monthly_report_view_pager.addOnPageChangeListener(this)
    }

// ------------------------------------------>

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        ignoreNextPageSelectedEvent = true

        viewModel.onPageSelected(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    companion object {
        /**
         * Extra to add the the launch intent to specify that user comes from the notification (used to
         * show not the current month but the last one)
         */
        const val FROM_NOTIFICATION_EXTRA = "fromNotif"
    }
}
