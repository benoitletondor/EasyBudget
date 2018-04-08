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

package com.ajapplications.budgeteerbuddy.view.main.calendar;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ajapplications.budgeteerbuddy.model.db.DB;
import com.ajapplications.budgeteerbuddy.R;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * @author Benoit LETONDOR
 */
public class CalendarGridAdapter extends CaldroidGridAdapter
{
    private final DB db;

// ----------------------------------->

    public CalendarGridAdapter(@NonNull Context context, int month, int year, Map<String, Object> caldroidData, Map<String, Object> extraData)
    {
        super(context, month, year, caldroidData, extraData);

        db = new DB(context.getApplicationContext());
    }

    @Override
    protected void finalize() throws Throwable
    {
        db.close();

        super.finalize();
    }

// ----------------------------------->

    @Override
    public View getView(int position, View convertView, final ViewGroup parent)
    {
        final View cellView = convertView == null ? createView(parent) : convertView;

        ViewData viewData = (ViewData) cellView.getTag();

        // Get dateTime of this cell
        DateTime dateTime = this.datetimeList.get(position);
        boolean isToday = dateTime.equals(getToday());
        boolean isDisabled = (minDateTime != null && dateTime.lt(minDateTime)) || (maxDateTime != null && dateTime.gt(maxDateTime)) || (disableDates != null && disableDatesMap.containsKey(dateTime));
        boolean isOutOfMonth = dateTime.getMonth() != month;

        TextView tv1 = viewData.dayTextView;
        TextView tv2 = viewData.amountTextView;

        // Set today's date
        tv1.setText("" + dateTime.getDay());

        // Customize for disabled dates and date outside min/max dates
        if ( isDisabled )
        {
            if( !viewData.isDisabled )
            {
                tv1.setTextColor(ContextCompat.getColor(context, R.color.calendar_cell_disabled_text_color));
                tv2.setVisibility(View.INVISIBLE);
                cellView.setBackgroundResource(android.R.color.white);

                viewData.isDisabled = true;
            }
        }
        else if( viewData.isDisabled ) // Reset all view params
        {
            tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
            tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
            tv2.setText("");
            tv2.setVisibility(View.VISIBLE);
            cellView.setBackgroundResource(R.drawable.custom_grid_cell_drawable);

            viewData.isDisabled = false;
            viewData.isSelected = false;
            viewData.isToday = false;
            viewData.containsExpenses = false;
            viewData.colorIndicatorMarginForToday = false;
            viewData.isOutOfMonth = false;
        }

        if( !isDisabled )
        {
            if( isOutOfMonth )
            {
                if( !viewData.isOutOfMonth )
                {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.divider));
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.divider));

                    viewData.isOutOfMonth = true;
                }
            }
            else if( viewData.isOutOfMonth )
            {
                tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));

                viewData.isOutOfMonth = false;
            }

            // Today's cell
            if( isToday )
            {
                // Customize for selected dates
                if (selectedDates != null && selectedDatesMap.containsKey(dateTime))
                {
                    if( !viewData.isToday || !viewData.isSelected )
                    {
                        cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_selected_drawable);

                        viewData.isToday = true;
                        viewData.isSelected = true;
                    }
                }
                else if( !viewData.isToday || viewData.isSelected )
                {
                    cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_drawable);

                    viewData.isToday = true;
                    viewData.isSelected = false;
                }
            }
            else
            {
                // Customize for selected dates
                if (selectedDates != null && selectedDatesMap.containsKey(dateTime))
                {
                    if( viewData.isToday || !viewData.isSelected )
                    {
                        cellView.setBackgroundResource(R.drawable.custom_grid_cell_selected_drawable);

                        viewData.isToday = false;
                        viewData.isSelected = true;
                    }
                }
                else if( viewData.isToday || viewData.isSelected )
                {
                    cellView.setBackgroundResource(R.drawable.custom_grid_cell_drawable);

                    viewData.isToday = false;
                    viewData.isSelected = false;
                }
            }

            final Date date = new Date(dateTime.getMilliseconds(TimeZone.getDefault()));
            if( db.hasExpensesForDay(date) )
            {
                double balance = db.getBalanceForDay(date);

                if( !viewData.containsExpenses )
                {
                    tv2.setVisibility(View.VISIBLE);

                    viewData.containsExpenses = true;
                }

                tv2.setText(String.valueOf(-(int) balance));

                if( balance > 0 )
                {
                    tv1.setTextColor(ContextCompat.getColor(context, isOutOfMonth ? R.color.budget_red_out : R.color.budget_red));
                }
                else
                {
                    tv1.setTextColor(ContextCompat.getColor(context, isOutOfMonth ? R.color.budget_green_out : R.color.budget_green));
                }
            }
            else if( viewData.containsExpenses )
            {
                tv2.setVisibility(View.INVISIBLE);

                if( !isOutOfMonth )
                {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
                }
                else
                {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.divider));
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.divider));
                }

                viewData.containsExpenses = false;
            }
        }

        cellView.setTag(viewData);
        return cellView;
    }

    /**
     * Inflate a new cell view and attach ViewData as tag
     *
     * @param parent
     * @return
     */
    private View createView(ViewGroup parent)
    {
        View v = LayoutInflater.from(context).inflate(R.layout.custom_grid_cell, parent, false);
        ViewData viewData = new ViewData();

        viewData.dayTextView = (TextView) v.findViewById(R.id.grid_cell_tv1);
        viewData.amountTextView = (TextView) v.findViewById(R.id.grid_cell_tv2);

        v.setTag(viewData);

        return v;
    }

// --------------------------------------->

    /**
     * Object that represent data of a cell for optimization purpose
     */
    public static class ViewData
    {
        /**
         * TextView that contains the day
         */
        public TextView dayTextView;
        /**
         * TextView that contains the amount of money for the day
         */
        public TextView amountTextView;

        /**
         * Is this cell a disabled date
         */
        public boolean isDisabled                   = false;
        /**
         * Is this cell out of the current month
         */
        public boolean isOutOfMonth                 = false;
        /**
         * Is this cell today's cell
         */
        public boolean isToday                      = false;
        /**
         * Is this cell selected
         */
        public boolean isSelected                   = false;
        /**
         * Does this cell contain expenses
         */
        public boolean containsExpenses             = false;
        /**
         * Are color indicator margin set for today
         */
        public boolean colorIndicatorMarginForToday = false;
    }
}
