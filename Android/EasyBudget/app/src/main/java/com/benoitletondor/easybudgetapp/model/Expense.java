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

package com.benoitletondor.easybudgetapp.model;

import android.support.annotation.NonNull;

import com.benoitletondor.easybudgetapp.helper.DateHelper;

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
     * Amount of this expense (can be < 0).
     */
    private double  amount;
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
    public Expense(@NonNull String title, double amount, @NonNull Date date)
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
    public Expense(@NonNull String title, double amount, @NonNull Date date, Long monthlyId)
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
    public Expense(Long id, @NonNull String title, double amount, @NonNull Date date, Long monthlyId)
    {
        this.id = id;

        if ( title.isEmpty() )
        {
            throw new IllegalArgumentException("title is empty or null");
        }

        this.title = title;

        if( amount == 0 )
        {
            throw new IllegalArgumentException("amount should be != 0");
        }

        this.amount = amount;
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

    @NonNull
    public String getTitle()
    {
        return title;
    }

    public void setTitle(@NonNull String title)
    {
        this.title = title;
    }

    @NonNull
    public Date getDate()
    {
        return date;
    }

    public void setDate(@NonNull Date date)
    {
        this.date = DateHelper.cleanDate(date);
    }

    public double getAmount()
    {
        return amount;
    }

    public void setAmount(double amount)
    {
        this.amount = amount;
    }

    public boolean isRevenue()
    {
        return amount < 0;
    }
}
