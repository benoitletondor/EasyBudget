/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.model.db;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.benoitletondor.easybudgetapp.helper.DateHelper;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.model.Expense;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache for SQLite database
 *
 * @author Benoit LETONDOR
 */
public class DBCache
{
    /**
     * Saved application context
     */
    private final Context context;
    /**
     * Map that contains expenses saved per day
     */
    private final SimpleArrayMap<Date, List<Expense>> expenses = new SimpleArrayMap<>();
    /**
     * Map that contains balances saved per day
     */
    private final SimpleArrayMap<Date, Double> balances = new SimpleArrayMap<>();
    /**
     * Single thread executor to load data from DB
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

// ------------------------------------->

    /**
     *
     * @param context
     */
    private DBCache(@NonNull Context context)
    {
        this.context = context.getApplicationContext();
    }

    /**
     * Load data for the given month if not already cached
     *
     * @param date the month we wanna load data for (no need to clear the date before)
     */
    public void loadMonth(@NonNull Date date)
    {
        Logger.debug("DBCache: Request to cache month: "+date);

        executor.execute(new LoadMonthRunnable(context, date));
        executor.execute(new LoadBalanceMonthRunnable(context, date));
    }

    /**
     * Instantly refresh cached data for the given day
     *
     * @param db database link
     * @param date cleaned date for the day
     */
    public void refreshForDay(@NonNull DB db, @NonNull Date date)
    {
        Logger.debug("DBCache: Refreshing for day: "+date);

        synchronized (balances)
        {
            balances.clear(); // TODO be smarter than delete all ?
        }

        synchronized (expenses)
        {
            expenses.put(DateHelper.cleanGMTDate(date), db.getExpensesForDay(date, false));
        }
    }

    /**
     * Instantly wipe all cached data
     */
    public void wipeAll()
    {
        Logger.debug("DBCache: Refreshing all");

        synchronized (balances)
        {
            balances.clear();
        }

        synchronized (expenses)
        {
            expenses.clear();
        }
    }

// ------------------------------------->

    /**
     * Get cached expenses for the day
     *
     * @param date cleaned date for the day
     * @return list of expense if cached data is available, null otherwise
     */
    @Nullable
    public List<Expense> getExpensesForDay(@NonNull Date date)
    {
        synchronized (expenses)
        {
            if( expenses.containsKey(date) )
            {
                return expenses.get(date);
            }

            executor.execute(new LoadMonthRunnable(context, date));
            return null;
        }
    }

    /**
     * Does this day contains expense (if cached)
     *
     * @param date cleaned date for the day
     * @return true or false if data is cached, null otherwise
     */
    @Nullable
    public Boolean hasExpensesForDay(@NonNull Date date)
    {
        synchronized (expenses)
        {
            List<Expense> expensesForDay = expenses.get(date);
            if( expensesForDay == null )
            {
                executor.execute(new LoadMonthRunnable(context, date));
                return null;
            }

            return !expensesForDay.isEmpty();
        }
    }

    /**
     * Get balance for the given day if cached
     *
     * @param day cleaned date for the day
     * @return balance if cached, null otherwise
     */
    @Nullable
    public Double getBalanceForDay(@NonNull Date day)
    {
        synchronized (balances)
        {
            if( balances.containsKey(day) )
            {
                return balances.get(day);
            }

            executor.execute(new LoadBalanceMonthRunnable(context, day));
            return null;
        }
    }

// --------------------------------------->

    /**
     * Runnable that loads data for a month in cache
     */
    private class LoadMonthRunnable implements Runnable
    {
        /**
         * Date containing the month to load
         */
        private Date month;
        /**
         * Saved context to get DB
         */
        private Context context;

        private LoadMonthRunnable(@NonNull Context context, @NonNull Date month)
        {
            this.month = month;
            this.context = context;
        }

        @Override
        public void run()
        {
            DB db = null;
            try
            {
                // Init a calendar to the given date, setting the day of month to 1
                Calendar cal = Calendar.getInstance();
                cal.setTime(DateHelper.cleanDate(month));
                cal.set(Calendar.DAY_OF_MONTH, 1);

                synchronized (expenses)
                {
                    if (expenses.containsKey(DateHelper.cleanGMTDate(cal.getTime())))
                    {
                        return;
                    }
                }

                db = new DB(context.getApplicationContext());

                // Save the month we wanna load cache for
                int month = cal.get(Calendar.MONTH);

                Logger.debug("DBCache: Caching data for month: "+month);

                // Iterate over day of month (while are still on that month)
                while( cal.get(Calendar.MONTH) == month )
                {
                    Date date = cal.getTime();
                    List<Expense> expensesForDay = db.getExpensesForDay(date, false);

                    synchronized (expenses)
                    {
                        expenses.put(DateHelper.cleanGMTDate(date), expensesForDay);
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }

                Logger.debug("DBCache: Data cached for month: "+month);
            }
            finally
            {
                if( db != null )
                {
                    db.close();
                }
            }
        }
    }

    /**
     * Runnable that loads balance data for a month in cache
     */
    private class LoadBalanceMonthRunnable implements Runnable
    {
        /**
         * Date containing the month to load
         */
        private Date month;
        /**
         * Saved context to get DB
         */
        private Context context;

        private LoadBalanceMonthRunnable(@NonNull Context context, @NonNull Date month)
        {
            this.month = month;
            this.context = context;
        }

        @Override
        public void run()
        {
            DB db = null;
            try
            {
                // Init a calendar to the given date, setting the day of month to 1
                Calendar cal = Calendar.getInstance();
                cal.setTime(DateHelper.cleanDate(month));
                cal.set(Calendar.DAY_OF_MONTH, 1);

                synchronized (balances)
                {
                    if (balances.containsKey(DateHelper.cleanGMTDate(cal.getTime())))
                    {
                        return;
                    }
                }

                db = new DB(context.getApplicationContext());

                // Save the month we wanna load cache for
                int month = cal.get(Calendar.MONTH);

                Logger.debug("DBCache: Caching balance data for month: "+month);

                // Iterate over day of month (while are still on that month)
                while( cal.get(Calendar.MONTH) == month )
                {
                    Date date = cal.getTime();
                    double balanceForDay = db.getBalanceForDay(date, false);

                    synchronized (balances)
                    {
                        balances.put(DateHelper.cleanGMTDate(date), balanceForDay);
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }

                Logger.debug("DBCache: Data balance cached for month: "+month);
            }
            finally
            {
                if( db != null )
                {
                    db.close();
                }
            }
        }
    }

// --------------------------------------->

    /**
     * Singleton instance
     */
    private static DBCache instance;

    /**
     * Instance accessor
     *
     * @param context
     * @return
     */
    public synchronized static DBCache getInstance(@NonNull Context context)
    {
        if( instance == null )
        {
            instance = new DBCache(context);
        }

        return instance;
    }
}
