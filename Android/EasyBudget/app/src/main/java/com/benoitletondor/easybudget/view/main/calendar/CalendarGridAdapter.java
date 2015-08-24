package com.benoitletondor.easybudget.view.main.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;
import com.benoitletondor.easybudget.model.db.DB;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * @author Benoit LETONDOR
 */
public class CalendarGridAdapter extends CaldroidGridAdapter
{
    private final DB db;

// ----------------------------------->

    public CalendarGridAdapter(Context context, int month, int year, HashMap<String, Object> caldroidData, HashMap<String, Object> extraData)
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
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View cellView = convertView;

        // For reuse
        if (convertView == null)
        {
            cellView = createView(parent);
        }

        ViewData viewData = (ViewData) cellView.getTag();

        // Get dateTime of this cell
        DateTime dateTime = this.datetimeList.get(position);
        boolean isToday = dateTime.equals(getToday());
        boolean isDisabled = (minDateTime != null && dateTime.lt(minDateTime)) || (maxDateTime != null && dateTime.gt(maxDateTime)) || (disableDates != null && disableDatesMap.containsKey(dateTime));
        boolean isOutOfMonth = dateTime.getMonth() != month;

        TextView tv1 = viewData.dayTextView;
        TextView tv2 = viewData.amountTextView;
        View cellColorIndicator = viewData.cellColorIndicator;

        // Set today's date
        tv1.setText("" + dateTime.getDay());

        // Customize for disabled dates and date outside min/max dates
        if ( isDisabled )
        {
            if( !viewData.isDisabled )
            {
                tv1.setTextColor(context.getColor(R.color.calendar_cell_disabled_text_color));
                tv2.setVisibility(View.INVISIBLE);
                cellColorIndicator.setVisibility(View.GONE);
                cellView.setBackgroundResource(android.R.color.white);

                viewData.isDisabled = true;
            }
        }
        else if( viewData.isDisabled ) // Reset all view params
        {
            tv1.setTextColor(context.getColor(R.color.primary_text));
            tv2.setTextColor(context.getColor(R.color.secondary_text));
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
                    tv1.setTextColor(context.getColor(R.color.divider));
                    tv2.setTextColor(context.getColor(R.color.divider));

                    viewData.isOutOfMonth = true;
                }
            }
            else if( viewData.isOutOfMonth )
            {
                tv1.setTextColor(context.getColor(R.color.primary_text));
                tv2.setTextColor(context.getColor(R.color.secondary_text));

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

            Date date = new Date(dateTime.getMilliseconds(TimeZone.getTimeZone("UTC")));
            if( db.hasExpensesForDay(date) )
            {
                int balance = db.getBalanceForDay(date);

                if( !viewData.containsExpenses )
                {
                    tv2.setVisibility(View.VISIBLE);
                    cellColorIndicator.setVisibility(View.VISIBLE);

                    viewData.containsExpenses = true;
                }

                tv2.setText(String.valueOf(-balance));

                if( balance > 0 )
                {
                    cellColorIndicator.setBackgroundResource(R.color.budget_red);
                }
                else if( balance <= 0 )
                {
                    cellColorIndicator.setBackgroundResource(R.color.budget_green);
                }

                // Apply margin to the color indicator if it's today's cell since there's a border
                if( isToday && !viewData.colorIndicatorMarginForToday )
                {
                    int marginDimen = context.getResources().getDimensionPixelOffset(R.dimen.grid_cell_today_border_size);

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellColorIndicator.getLayoutParams());
                    params.setMargins(0, marginDimen, marginDimen, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    cellColorIndicator.setLayoutParams(params);

                    viewData.colorIndicatorMarginForToday = true;
                }
                else if( !isToday && viewData.colorIndicatorMarginForToday )
                {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellColorIndicator.getLayoutParams());
                    params.setMargins(0, 0, 0, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    cellColorIndicator.setLayoutParams(params);

                    viewData.colorIndicatorMarginForToday = false;
                }
            }
            else if( viewData.containsExpenses )
            {
                cellColorIndicator.setVisibility(View.GONE);
                tv2.setVisibility(View.INVISIBLE);

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
        viewData.cellColorIndicator = v.findViewById(R.id.cell_color_indicator);

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
         * View that display the color indicator of amount of money for the day
         */
        public View     cellColorIndicator;

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
