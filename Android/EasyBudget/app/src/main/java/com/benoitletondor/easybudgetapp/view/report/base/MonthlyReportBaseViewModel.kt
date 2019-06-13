package com.benoitletondor.easybudgetapp.view.report.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.DateHelper
import com.benoitletondor.easybudgetapp.parameters.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MonthlyReportBaseViewModel(private val parameters: Parameters) : ViewModel() {
    /**
     * The current selected position
     */
    val selectedPositionLiveData = MutableLiveData<Triple<Int, Date, Boolean>>()
    val datesLiveData = MutableLiveData<List<Date>>()

    fun loadData(fromNotification: Boolean) {
        viewModelScope.launch {
            val dates = withContext(Dispatchers.IO) {
                return@withContext DateHelper.getListOfMonthsAvailableForUser(parameters)
            }

            datesLiveData.value = dates
            if( !fromNotification || dates.size == 1) {
                selectedPositionLiveData.value = Triple(dates.size - 1, dates[dates.size - 1], true)
            } else {
                selectedPositionLiveData.value = Triple(dates.size - 2, dates[dates.size - 2], false)
            }
        }
    }

    fun onPreviousMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if (selectedPosition > 0) {
            selectedPositionLiveData.value = Triple(selectedPosition - 1, dates[selectedPosition - 1], false)
        }
    }

    fun onNextMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if ( selectedPosition < dates.size - 1 ) {
            selectedPositionLiveData.value = Triple(selectedPosition + 1, dates[selectedPosition + 1], dates.size == selectedPosition + 2)
        }
    }

    fun onPageSelected(position: Int) {
        val dates = datesLiveData.value ?: return

        selectedPositionLiveData.value = Triple(position, dates[position], dates.size == position + 1)
    }
}