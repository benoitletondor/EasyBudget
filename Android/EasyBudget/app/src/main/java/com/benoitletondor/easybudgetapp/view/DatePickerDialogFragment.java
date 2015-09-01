package com.benoitletondor.easybudgetapp.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;

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
    public DatePickerDialogFragment(@NonNull Date originalDate, @NonNull DatePickerDialog.OnDateSetListener listener)
    {
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
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), listener, year, month, day);
        dialog.getDatePicker().setMinDate(Parameters.getInstance(getActivity()).getLong(ParameterKeys.INIT_DATE, new Date().getTime()));
        return dialog;
    }

    @Override
    public void onDestroyView()
    {
        listener = null;

        super.onDestroyView();
    }
}
