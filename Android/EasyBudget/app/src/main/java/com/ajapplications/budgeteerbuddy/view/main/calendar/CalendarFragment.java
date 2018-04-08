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

package com.ajapplications.budgeteerbuddy.view.main.calendar;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.Date;

/**
 * @author Benoit LETONDOR
 */
public class CalendarFragment extends CaldroidFragment
{
    private Date selectedDate;

// --------------------------------------->

    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year)
    {
        return new CalendarGridAdapter(getActivity(), month, year, getCaldroidData(), extraData);
    }

    @Override
    public void setSelectedDates(Date fromDate, Date toDate)
    {
        this.selectedDate = fromDate;
        super.setSelectedDates(fromDate, toDate);
        try
        {
            super.moveToDate(fromDate);
        }
        catch (Exception ignored){} // Exception that occurs if we call this code before the calendar being initialized
    }

    public Date getSelectedDate()
    {
        return selectedDate;
    }
}
