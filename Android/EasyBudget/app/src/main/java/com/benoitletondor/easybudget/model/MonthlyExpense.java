package com.benoitletondor.easybudget.model;

import com.benoitletondor.easybudget.helper.DateHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Object that represent a expense that occur on every month
 *
 * @author Benoit LETONDOR
 */
public class MonthlyExpense extends Expense
{
    /**
     * Amount of the monthly expense at the beginning (before any edit)
     */
    private int  startAmount;
    /**
     * Day of month of this expense (computed at runtime from the StartDate)
     */
    private int  dayOfMonth;
    /**
     * Start date of this recurring expense (Should not be updated)
     */
    private Date startDate;
    /**
     * End date of this recurring expense
     */
    private Date endDate;
    /**
     * List of modifications that have been made to this expense.
     */
    private Map<Date, Integer> modifications = new HashMap<>();

// ---------------------------------->

    /**
     *
     * @param title
     * @param startAmount
     * @param startDate
     */
    public MonthlyExpense(String title, int startAmount, Date startDate)
    {
        this(title, startAmount, startDate, null);
    }

    /**
     *
     * @param title
     * @param startAmount
     * @param startDate
     * @param endDate
     */
    public MonthlyExpense(String title, int startAmount, Date startDate, Date endDate)
    {
        super(title);

        if (startAmount == 0)
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

        // If the endDate is null, it's set to 1000 year from now
        if( endDate == null )
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1000);

            endDate = cal.getTime();
        }

        this.endDate = DateHelper.cleanDate(endDate);
    }

    /**
     *
     * @param title
     * @param startAmount
     * @param startDate
     * @param endDate
     * @param modifications
     */
    public MonthlyExpense(String title, int startAmount, Date startDate, Date endDate, Map<Date, Integer> modifications)
    {
        this(title, startAmount, startDate, endDate);

        if( modifications == null )
        {
            throw new NullPointerException("modifications==null");
        }

        this.modifications = modifications;
    }

// ---------------------------------->

    /**
     * Add a modification to this expense
     *
     * @param startingDate
     * @param newAmount
     */
    public void addModification(Date startingDate, int newAmount)
    {
        if( newAmount == 0 )
        {
            throw new IllegalArgumentException("amount should be != 0");
        }

        if( startingDate.before(startDate) )
        {
            throw new IllegalArgumentException("starting date should be > start date");
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

    /**
     * Get the amount for a given date
     *
     * @param date
     * @return
     */
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

    /**
     *
     * @return
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     *
     * @return
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     *
     * @return
     */
    public int getStartAmount()
    {
        return startAmount;
    }

    /**
     *
     * @return
     */
    public int getDayOfMonth()
    {
        return dayOfMonth;
    }

// ---------------------------------->

    /**
     * Helper to serialize modifications to a json string
     *
     * @param expense
     * @return
     * @throws JSONException
     */
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

    /**
     * Helper to deserialize modification from a json string
     *
     * @param jsonString
     * @return
     * @throws JSONException
     */
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
