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
 * Object that represent a expense that occur on every month
 *
 * @author Benoit LETONDOR
 */
public class MonthlyExpense implements Serializable
{
    /**
     * DB id of this expense (can be null)
     */
    private Long id;
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
    private double  amount;
    /**
     * Is this expense modified (Not implemented yet)
     */
    private boolean modified = false;

// ---------------------------------->

    /**
     *
     * @param title
     * @param startAmount
     * @param recurringDate
     */
    public MonthlyExpense(@NonNull String title, double startAmount, @NonNull Date recurringDate)
    {
        if (startAmount == 0)
        {
            throw new IllegalArgumentException("startAmount should be != 0");
        }

        this.amount = startAmount;
        this.title = title;
        this.recurringDate = DateHelper.cleanDate(recurringDate);
    }

    /**
     *
     * @param id
     * @param title
     * @param startAmount
     * @param recurringDate
     * @param modified
     */
    public MonthlyExpense(Long id, @NonNull String title, double startAmount, @NonNull Date recurringDate, boolean modified)
    {
        this(title, startAmount, recurringDate);

        this.id = id;
        this.modified = modified;
    }

// ---------------------------------->

    /**
     *
     * @return
     */
    @NonNull
    public String getTitle()
    {
        return title;
    }

    /**
     *
     * @return
     */
    @NonNull
    public Date getRecurringDate()
    {
        return recurringDate;
    }

    /**
     *
     * @return
     */
    public double getAmount()
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

    /**
     *
     * @param id
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public Long getId()
    {
        return id;
    }

}
