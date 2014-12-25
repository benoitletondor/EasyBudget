package com.benoitletondor.easybudget.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public MonthlyExpense(int startAmount, Date startDate, Date endDate, Map<Date, Integer> modifications)
    {
        this(startAmount, startDate, endDate);

        if( modifications == null )
        {
            throw new NullPointerException("modifications==null");
        }

        this.modifications = modifications;
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

// ---------------------------------->

    public static String modificationsToJson(MonthlyExpense expense) throws JSONException
    {
        JSONArray array = new JSONArray();

        for( Date modificationDate : expense.modifications.keySet() )
        {
            Integer amount = expense.modifications.get(modificationDate);

            JSONObject modifJson = new JSONObject();
            modifJson.put("d", modificationDate.getTime());
            modifJson.put("a", amount);
            array.put(modifJson);
        }

        return array.toString();
    }

    public static Map<Date, Integer> jsonToModifications(String jsonString) throws JSONException
    {
        JSONArray array = new JSONArray(jsonString);

        Map<Date, Integer> modifications = new HashMap<>();

        for(int i=0; i<array.length(); i++)
        {
            JSONObject modifJson = array.getJSONObject(i);
            modifications.put(new Date(modifJson.getInt("d")), modifJson.getInt("a"));
        }

        return modifications;
    }
}
