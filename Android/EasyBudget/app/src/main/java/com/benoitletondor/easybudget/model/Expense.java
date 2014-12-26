package com.benoitletondor.easybudget.model;

/**
 * Common parent of all expenses. Used for the ViewAdapter
 *
 * @author Benoit LETONDOR
 */
public abstract class Expense
{
    /**
     * Title of the expense
     */
    protected String title;

// --------------------------------->

    /**
     *
     * @param title
     */
    public Expense(String title)
    {
        if (title == null || title.isEmpty())
        {
            throw new IllegalArgumentException("title is empty or null");
        }

        this.title = title;
    }

// --------------------------------->

    /**
     *
     * @return
     */
    public String getTitle()
    {
        return title;
    }
}
