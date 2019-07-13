/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.report.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.getListOfMonthsAvailableForUser
import com.benoitletondor.easybudgetapp.parameters.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MonthlyReportBaseViewModel(private val parameters: Parameters) : ViewModel() {
    /**
     * The current selected position
     */
    val selectedPositionLiveData = MutableLiveData<MonthlyReportSelectedPosition>()
    val datesLiveData = MutableLiveData<List<Date>>()

    fun loadData(fromNotification: Boolean) {
        viewModelScope.launch {
            val dates = withContext(Dispatchers.IO) {
                return@withContext parameters.getListOfMonthsAvailableForUser()
            }

            datesLiveData.value = dates
            if( !fromNotification || dates.size == 1) {
                selectedPositionLiveData.value = MonthlyReportSelectedPosition(dates.size - 1, dates[dates.size - 1], true)
            } else {
                selectedPositionLiveData.value = MonthlyReportSelectedPosition(dates.size - 2, dates[dates.size - 2], false)
            }
        }
    }

    fun onPreviousMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if (selectedPosition > 0) {
            selectedPositionLiveData.value = MonthlyReportSelectedPosition(selectedPosition - 1, dates[selectedPosition - 1], false)
        }
    }

    fun onNextMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if ( selectedPosition < dates.size - 1 ) {
            selectedPositionLiveData.value = MonthlyReportSelectedPosition(selectedPosition + 1, dates[selectedPosition + 1], dates.size == selectedPosition + 2)
        }
    }

    fun onPageSelected(position: Int) {
        val dates = datesLiveData.value ?: return

        selectedPositionLiveData.value = MonthlyReportSelectedPosition(position, dates[position], dates.size == position + 1)
    }
}

data class MonthlyReportSelectedPosition(val position: Int, val date: Date, val latest: Boolean)