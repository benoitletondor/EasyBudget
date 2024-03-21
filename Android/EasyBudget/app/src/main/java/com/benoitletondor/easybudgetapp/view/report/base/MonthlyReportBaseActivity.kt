/*
 *   Copyright 2024 Benoit LETONDOR
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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityMonthlyReportBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.getMonthTitle
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.removeButtonBorder
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportFragment
import com.benoitletondor.easybudgetapp.view.report.export.ExportReportActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.YearMonth

/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MonthlyReportBaseActivity : BaseActivity<ActivityMonthlyReportBinding>(), ViewPager.OnPageChangeListener {

    private val viewModel: MonthlyReportBaseViewModel by viewModels()

    private var ignoreNextPageSelectedEvent: Boolean = false

    override fun createBinding(): ActivityMonthlyReportBinding = ActivityMonthlyReportBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.monthlyReportPreviousMonthButton.text = "<"
        binding.monthlyReportNextMonthButton.text = ">"

        binding.monthlyReportPreviousMonthButton.setOnClickListener {
            viewModel.onPreviousMonthButtonClicked()
        }

        binding.monthlyReportNextMonthButton.setOnClickListener {
            viewModel.onNextMonthButtonClicked()
        }

        binding.monthlyReportPreviousMonthButton.removeButtonBorder()
        binding.monthlyReportNextMonthButton.removeButtonBorder()

        var loadedMonths: List<YearMonth> = emptyList()
        lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            when(state) {
                is MonthlyReportBaseViewModel.State.Loaded -> {
                    binding.monthlyReportProgressBar.visibility = View.GONE
                    binding.monthlyReportContent.visibility = View.VISIBLE

                    if (state.months != loadedMonths) {
                        loadedMonths = state.months
                        configureViewPager(state.months)
                    }

                    if( !ignoreNextPageSelectedEvent ) {
                        binding.monthlyReportViewPager.setCurrentItem(state.selectedPosition.position, true)
                    }

                    ignoreNextPageSelectedEvent = false

                    binding.monthlyReportMonthTitleTv.text = state.selectedPosition.month.getMonthTitle(this)

                    // Last and first available month
                    val isFirstMonth = state.selectedPosition.position == 0

                    binding.monthlyReportNextMonthButton.isEnabled = !state.selectedPosition.latest
                    binding.monthlyReportPreviousMonthButton.isEnabled = !isFirstMonth
                }
                MonthlyReportBaseViewModel.State.Loading -> {
                    binding.monthlyReportProgressBar.visibility = View.VISIBLE
                    binding.monthlyReportContent.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                is MonthlyReportBaseViewModel.Event.OpenExport -> startActivity(ExportReportActivity.createIntent(this, event.month))
                MonthlyReportBaseViewModel.Event.RefreshMenu -> invalidateOptionsMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_monthly_report, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu == null) {
            return false
        }

        if (!viewModel.shouldShowExportButton()) {
            menu.removeItem(R.id.action_export)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.action_export) {
            viewModel.onExportButtonClicked()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<YearMonth>) {
        binding.monthlyReportViewPager.removeOnPageChangeListener(this)

        binding.monthlyReportViewPager.offscreenPageLimit = 0
        binding.monthlyReportViewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return MonthlyReportFragment.newInstance(dates[position])
            }

            override fun getCount(): Int {
                return dates.size
            }
        }
        binding.monthlyReportViewPager.addOnPageChangeListener(this)
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
