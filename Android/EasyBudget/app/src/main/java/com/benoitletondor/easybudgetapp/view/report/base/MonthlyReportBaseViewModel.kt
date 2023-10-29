/*
 *   Copyright 2023 Benoit LETONDOR
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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.getListOfMonthsAvailableForUser
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity.Companion.FROM_NOTIFICATION_EXTRA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MonthlyReportBaseViewModel @Inject constructor(
    private val parameters: Parameters,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val fromNotification = savedStateHandle.get<Boolean>(FROM_NOTIFICATION_EXTRA) ?: false

    private val stateMutableFlow = MutableStateFlow<State>(State.Loading)
    val stateFlow: Flow<State> = stateMutableFlow

    init {
        viewModelScope.launch {
            stateMutableFlow.value = State.Loading

            val dates = withContext(Dispatchers.IO) {
                return@withContext parameters.getListOfMonthsAvailableForUser()
            }

            val selectedPosition = if( !fromNotification || dates.size == 1) {
                MonthlyReportSelectedPosition(dates.size - 1, dates[dates.size - 1], true)
            } else {
                MonthlyReportSelectedPosition(dates.size - 2, dates[dates.size - 2], false)
            }

            stateMutableFlow.value = State.Loaded(dates, selectedPosition)
        }
    }

    fun onPreviousMonthButtonClicked() {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        val position = loadedState.selectedPosition.position
        if (position > 0) {
            stateMutableFlow.value = loadedState.copy(
                selectedPosition = MonthlyReportSelectedPosition(position - 1, loadedState.dates[position - 1], false)
            )
        }
    }

    fun onNextMonthButtonClicked() {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        val position = loadedState.selectedPosition.position
        if ( position < loadedState.dates.size - 1 ) {
            stateMutableFlow.value = loadedState.copy(
                selectedPosition = MonthlyReportSelectedPosition(position + 1, loadedState.dates[position + 1], loadedState.dates.size == position + 2)
            )
        }
    }

    fun onPageSelected(position: Int) {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        stateMutableFlow.value = loadedState.copy(
            selectedPosition = MonthlyReportSelectedPosition(position, loadedState.dates[position], loadedState.dates.size == position + 1)
        )
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(val dates: List<LocalDate>, val selectedPosition: MonthlyReportSelectedPosition) : State()
    }
}

data class MonthlyReportSelectedPosition(val position: Int, val date: LocalDate, val latest: Boolean)