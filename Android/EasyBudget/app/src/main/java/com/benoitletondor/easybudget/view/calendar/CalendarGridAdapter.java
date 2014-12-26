package com.benoitletondor.easybudget.view.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.benoitletondor.easybudget.R;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.HashMap;

import hirondelle.date4j.DateTime;

/**
 * @author Benoit LETONDOR
 */
public class CalendarGridAdapter extends CaldroidGridAdapter
{

    public CalendarGridAdapter(Context context, int month, int year, HashMap<String, Object> caldroidData, HashMap<String, Object> extraData)
    {
        super(context, month, year, caldroidData, extraData);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View cellView = convertView;

        // For reuse
        if (convertView == null)
        {
            cellView = createView(parent);
        }

        // Get dateTime of this cell
        DateTime dateTime = this.datetimeList.get(position);

        TextView tv1 = (TextView) cellView.findViewById(R.id.grid_cell_tv1);
        TextView tv2 = (TextView) cellView.findViewById(R.id.grid_cell_tv2);

        // Customize for disabled dates and date outside min/max dates
        if ((minDateTime != null && dateTime.lt(minDateTime))
                || (maxDateTime != null && dateTime.gt(maxDateTime))
                || (disableDates != null && disableDatesMap.containsKey(dateTime))
                || (dateTime.getMonth() != month) )
        {

            tv1.setTextColor(context.getResources().getColor(R.color.divider));
            tv2.setTextColor(context.getResources().getColor(R.color.divider));
        }
        else
        {
            tv1.setTextColor(context.getResources().getColor(R.color.primary_text));
            tv1.setTextColor(context.getResources().getColor(R.color.secondary_text));
        }

        // Today's cell
        if( dateTime.equals(getToday()) )
        {
            // Customize for selected dates
            if (selectedDates != null && selectedDatesMap.containsKey(dateTime))
            {
                cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_selected_drawable);
            }
            else
            {
                cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_drawable);
            }
        }
        else
        {
            // Customize for selected dates
            if (selectedDates != null && selectedDatesMap.containsKey(dateTime))
            {
                cellView.setBackgroundResource(R.drawable.custom_grid_cell_selected_drawable);
            }
            else
            {
                cellView.setBackgroundResource(R.drawable.custom_grid_cell_drawable);
            }
        }

        tv1.setText("" + dateTime.getDay());
        tv2.setText("Hi");

        return cellView;
    }

    /**
     * Inflate a new cell view
     *
     * @param parent
     * @return
     */
    private View createView(ViewGroup parent)
    {
        return LayoutInflater.from(context).inflate(R.layout.custom_grid_cell, parent, false);
    }
}
