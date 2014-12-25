package com.benoitletondor.easybudget.model;

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
}
