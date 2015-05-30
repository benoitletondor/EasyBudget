package com.benoitletondor.easybudget.model;

import com.benoitletondor.easybudget.helper.DateHelper;

import java.io.Serializable;
import java.util.Date;

/**
 * Object that represent an expense.
 *
 * @author Benoit LETONDOR
 */
public class Expense implements Serializable
{
    /**
     * DB id of this expense (can be null)
     */
    private Long id;
    /**
     * Title of the expense
     */
    private String  title;
    /**
     * Amount of this expense (can be < 0)
     */
    private int     amount;
    /**
     * Date of the expense
     */
    private Date    date;
    /**
     * Id of the recurring monthly expense (can be null)
     */
    private Long monthlyId;

// --------------------------------->

    /**
     *
     * @param title
     * @param amount
     * @param date
     */
    public Expense(String title, int amount, Date date)
    {
        this(null, title, amount, date, null);
    }

    /**
     *
     * @param title
     * @param amount
     * @param date
     * @param monthlyId
     */
    public Expense(String title, int amount, Date date, Long monthlyId)
    {
        this(null, title, amount, date, monthlyId);
    }

    /**
     *
     * @param id
     * @param title
     * @param amount
     * @param date
     * @param monthlyId
     */
    public Expense(Long id, String title, int amount, Date date, Long monthlyId)
    {
        this.id = id;

        if (title == null || title.isEmpty())
        {
            throw new IllegalArgumentException("title is empty or null");
        }

        this.title = title;

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
        this.monthlyId = monthlyId;
    }

// --------------------------------->

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getMonthlyId()
    {
        return monthlyId;
    }

    public void setMonthlyId(Long monthlyId)
    {
        this.monthlyId = monthlyId;
    }

    public boolean isMonthly()
    {
        return monthlyId != null;
    }

    /**
     *
     * @return
     */
    public String getTitle()
    {
        return title;
    }

    public Date getDate()
    {
        return date;
    }

    public int getAmount()
    {
        return amount;
    }
}
