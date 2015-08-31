package com.benoitletondor.easybudgetapp.view.main.calendar;

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
    }

    public Date getSelectedDate()
    {
        return selectedDate;
    }
}
