package com.benoitletondor.easybudget.model;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Benoit LETONDOR
 */
public class Expense
{
    private final int amount;

// ------------------------------------>

    public Expense(final int amount)
    {
        if( amount == 0 )
        {
            throw new IllegalArgumentException("amount should be != 0");
        }

        this.amount = amount;
    }

// ------------------------------------>

    public int getAmount()
    {
        return amount;
    }

    public boolean isRevenue()
    {
        return amount < 0;
    }

// ---------------------------------->

    public static Date cleanDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }
}
