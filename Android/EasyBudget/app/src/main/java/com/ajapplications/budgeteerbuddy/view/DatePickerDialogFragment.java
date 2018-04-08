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

package com.ajapplications.budgeteerbuddy.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.ajapplications.budgeteerbuddy.helper.ParameterKeys;
import com.ajapplications.budgeteerbuddy.helper.Parameters;

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
