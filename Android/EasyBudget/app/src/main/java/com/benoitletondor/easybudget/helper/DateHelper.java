package com.benoitletondor.easybudget.helper;

import java.util.Calendar;
import java.util.Date;

/**
 * Helper to work with dates
 *
 * @author Benoit LETONDOR
 */
public class DateHelper
{
    /**
     * Remove hour, minutes, seconds and ms data from a date.
     *
     * @param date
     * @return a new cleaned date
     */
    public static Date cleanDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        return cal.getTime();
    }

    /**
     * Return the day of month of a date
     *
     * @param date
     * @return
     */
    public static int getDayOfMonth(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_MONTH);
    }
}
