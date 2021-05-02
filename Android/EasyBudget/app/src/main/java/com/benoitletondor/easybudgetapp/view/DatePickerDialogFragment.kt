/*
 *   Copyright 2021 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.benoitletondor.easybudgetapp.helper.computeCalendarMinDateFromInitDate
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitTimestamp
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

/**
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class DatePickerDialogFragment(private val originalDate: Date, private val listener: DatePickerDialog.OnDateSetListener) : DialogFragment() {
    @Inject lateinit var parameters: Parameters

    constructor() : this(Date(), DatePickerDialog.OnDateSetListener { _, _, _, _ -> }) {
        throw RuntimeException("DatePickerDialogFragment is supposed to be instanciated with the date+listener constructor")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker
        val c = Calendar.getInstance()
        c.time = originalDate

        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        val dialog = DatePickerDialog(requireContext(), listener, year, month, day)
        dialog.datePicker.minDate = Date(parameters.getInitTimestamp()).computeCalendarMinDateFromInitDate().time
        return dialog
    }
}
