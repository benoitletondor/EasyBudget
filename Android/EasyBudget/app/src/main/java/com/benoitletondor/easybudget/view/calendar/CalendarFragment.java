package com.benoitletondor.easybudget.view.calendar;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;

/**
 * @author Benoit LETONDOR
 */
public class CalendarFragment extends CaldroidFragment
{
    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year)
    {
        return new CalendarGridAdapter(getActivity(), month, year, getCaldroidData(), extraData);
    }
}
