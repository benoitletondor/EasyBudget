package com.benoitletondor.easybudget.model;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Benoit LETONDOR
 */
public class MonthlyExpense extends Expense
{
    private Date startDate;
    private Date endDate;
    private Map<Date, Integer> modifications = new HashMap<>();

// ---------------------------------->

    public MonthlyExpense(int startAmount, Date startDate, Date endDate)
    {
        super(startAmount);

        if (startDate == null)
        {
            throw new NullPointerException("date==null");
        }

        this.startDate = cleanDate(startDate);
        this.endDate = cleanDate(endDate);
    }

// ---------------------------------->

    public void addModification(Date startingDate, int newAmount)
    {
        if( newAmount == 0 )
        {
            throw new IllegalArgumentException("amount should be != 0");
        }

        Date cleanedDate = cleanDate(startingDate);

        Iterator<Date> modificationDateIterator = modifications.keySet().iterator();
        while( modificationDateIterator.hasNext() )
        {
            Date modificationDate = modificationDateIterator.next();

            if( modificationDate.after(cleanedDate) )
            {
                modificationDateIterator.remove();
            }
        }

        modifications.put(cleanedDate, new Integer(newAmount));
    }

    public int getAmountForMonth(Date date)
    {
        Date cleanedDate = cleanDate(date);
        int amount = getAmount();

        if( modifications.isEmpty() )
        {
            return amount;
        }

        for( Date modificationDate : modifications.keySet() )
        {
            if( modificationDate.before(cleanedDate) )
            {
                amount = modifications.get(modificationDate);
            }
        }

        return amount;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

// ---------------------------------->

    private static Date cleanDate(Date date)
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
