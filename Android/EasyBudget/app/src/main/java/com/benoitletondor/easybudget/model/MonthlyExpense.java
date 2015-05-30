package com.benoitletondor.easybudget.model;

import com.benoitletondor.easybudget.helper.DateHelper;

import java.util.Date;

/**
 * Object that represent a expense that occur on every month
 *
 * @author Benoit LETONDOR
 */
public class MonthlyExpense
{
    /**
     * Title of this expense when created
     */
    private String  title;
    /**
     * Start date of this recurring expense (Should not be updated)
     */
    private Date    recurringDate;
    /**
     * Amount of this expense when created
     */
    private int     amount;
    /**
     * Is this expense modified
     */
    private boolean modified = false;

// ---------------------------------->

    /**
     *
     * @param title
     * @param startAmount
     * @param recurringDate
     */
    public MonthlyExpense(String title, int startAmount, Date recurringDate)
    {
        if (startAmount == 0)
        {
            throw new IllegalArgumentException("startAmount should be != 0");
        }

        this.amount = startAmount;

        if( title == null )
        {
            throw new IllegalArgumentException("title is empty or null");
        }

        this.title = title;

        if (recurringDate == null)
        {
            throw new NullPointerException("recurringDate==null");
        }

        this.recurringDate = DateHelper.cleanDate(recurringDate);
    }

    /**
     *
     * @param title
     * @param startAmount
     * @param recurringDate
     * @param modified
     */
    public MonthlyExpense(String title, int startAmount, Date recurringDate, boolean modified)
    {
        this(title, startAmount, recurringDate);

        this.modified = modified;
    }

// ---------------------------------->

    /**
     *
     * @return
     */
    public String getTitle()
    {
        return title;
    }

    /**
     *
     * @return
     */
    public Date getRecurringDate()
    {
        return recurringDate;
    }

    /**
     *
     * @return
     */
    public int getAmount()
    {
        return amount;
    }

    /**
     *
     * @return
     */
    public boolean isModified()
    {
        return modified;
    }

}
