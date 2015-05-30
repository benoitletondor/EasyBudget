package com.benoitletondor.easybudget.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Benoit LETONDOR
 */
public class DatePickerDialogFragment extends DialogFragment
{
    private Date originalDate;
    private DatePickerDialog.OnDateSetListener listener;

// ------------------------------------------>

    public DatePickerDialogFragment()
    {
        throw new RuntimeException("DatePickerDialogFragment is supposed to be instanciated with the date+listener constructor");
    }

    @SuppressLint("ValidFragment")
    public DatePickerDialogFragment(Date originalDate, DatePickerDialog.OnDateSetListener listener)
    {
        if( originalDate == null )
        {
            throw new NullPointerException("originalDate==null");
        }

        if( listener == null )
        {
            throw new NullPointerException("listener==null");
        }

        this.originalDate = originalDate;
        this.listener = listener;
    }

// ------------------------------------------>

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        c.setTime(originalDate);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), listener, year, month, day);
    }

    @Override
    public void onDestroyView()
    {
        listener = null;

        super.onDestroyView();
    }
}
