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

package com.benoitletondor.easybudgetapp.view.main.calendar

import com.benoitletondor.easybudgetapp.db.DB
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidGridAdapter
import org.koin.android.ext.android.inject

import java.util.Date

/**
 * @author Benoit LETONDOR
 */
class CalendarFragment : CaldroidFragment() {
    private var mSelectedDate = Date()

    private val db: DB by inject()

// --------------------------------------->

    override fun getNewDatesGridAdapter(month: Int, year: Int): CaldroidGridAdapter {
        return CalendarGridAdapter(context!!, db, month, year, getCaldroidData(), extraData)
    }

    override fun onDestroy() {
        db.close()

        super.onDestroy()
    }

    override fun setSelectedDates(fromDate: Date, toDate: Date) {
        this.mSelectedDate = fromDate
        super.setSelectedDates(fromDate, toDate)

        try {
            // Exception that occurs if we call this code before the calendar being initialized
            super.moveToDate(fromDate)
        } catch (ignored: Exception) { }
    }

    fun getSelectedDate() = mSelectedDate

    fun setFirstDayOfWeek(firstDayOfWeek: Int) {
        if (firstDayOfWeek != startDayOfWeek) {
            startDayOfWeek = firstDayOfWeek
            val weekdaysAdapter = getNewWeekdayAdapter(themeResource)
            weekdayGridView.adapter = weekdaysAdapter
            nextMonth()
            prevMonth()
        }
    }
}
