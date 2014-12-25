package com.benoitletondor.easybudget.model;

import com.benoitletondor.easybudget.helper.DateHelper;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Benoit LETONDOR
 */
public class OneTimeExpense extends Expense
{
    private int amount;
    private Date date;

// ------------------------------------>

    public OneTimeExpense(int amount, Date date)
    {
        if( amount == 0 )
        {
            throw new IllegalArgumentException("amount should be != 0");
        }

        this.amount = amount;

        if( date == null )
        {
            throw new NullPointerException("date==null");
        }

        this.date = DateHelper.cleanDate(date);
    }

// ------------------------------------>

    public Date getDate()
    {
        return date;
    }

    public int getAmount()
    {
        return amount;
    }
}
