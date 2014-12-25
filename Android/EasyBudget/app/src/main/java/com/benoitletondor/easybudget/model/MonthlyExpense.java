package com.benoitletondor.easybudget.model;

import com.benoitletondor.easybudget.helper.DateHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Benoit LETONDOR
 */
public class MonthlyExpense
{
    private int startAmount;
    private int dayOfMonth;
    private Date startDate;
    private Date endDate;
    private Map<Date, Integer> modifications = new HashMap<>();

// ---------------------------------->

    public MonthlyExpense(int startAmount, Date startDate, Date endDate)
    {
        if( startAmount == 0 )
        {
            throw new IllegalArgumentException("startAmount should be != 0");
        }

        this.startAmount = startAmount;

        if (startDate == null)
        {
            throw new NullPointerException("date==null");
        }

        this.startDate = DateHelper.cleanDate(startDate);
        this.dayOfMonth = DateHelper.getDayOfMonth(startDate);
        this.endDate = DateHelper.cleanDate(endDate);
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

        Date cleanedDate = DateHelper.cleanDate(startingDate);

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
        Date cleanedDate = DateHelper.cleanDate(date);
        int amount = startAmount;

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

    public int getStartAmount()
    {
        return startAmount;
    }

    public int getDayOfMonth()
    {
        return dayOfMonth;
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
