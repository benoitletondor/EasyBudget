/*
 *   Copyright 2023 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.main.account.calendar

import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.main.account.AccountFragment
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

import javax.inject.Inject

/**
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class CalendarFragment : CaldroidFragment() {
    private var mSelectedDate = LocalDate.now()

    @Inject lateinit var parameters: Parameters

// --------------------------------------->

    override fun getNewDatesGridAdapter(month: Int, year: Int): CaldroidGridAdapter {
        return CalendarGridAdapter(
            context = requireContext(),
            dataProvider = parentFragment as AccountFragment,
            parameters = parameters,
            month = month,
            year = year,
            caldroidData = getCaldroidData(),
            extraData = extraData
        )
    }

    override fun setSelectedDate(date: LocalDate) {
        this.mSelectedDate = date

        super.clearSelectedDates()
        super.setSelectedDate(date)

        try {
            // Exception that occurs if we call this code before the calendar being initialized
            super.moveToDate(date)
        } catch (ignored: Exception) { }
    }

    fun getSelectedDate(): LocalDate = mSelectedDate

    fun setFirstDayOfWeek(firstDayOfWeek: Int) {
        if (firstDayOfWeek != startDayOfWeek) {
            startDayOfWeek = firstDayOfWeek
            val weekdaysAdapter = getNewWeekdayAdapter(themeResource)
            weekdayGridView.adapter = weekdaysAdapter
            nextMonth()
            prevMonth()
        }
    }

    fun goToCurrentMonth() {
        try {
            // Exception that occurs if we call this code before the calendar being initialized
            super.moveToDate(LocalDate.now())
        } catch (ignored: Exception) { }
    }
}
