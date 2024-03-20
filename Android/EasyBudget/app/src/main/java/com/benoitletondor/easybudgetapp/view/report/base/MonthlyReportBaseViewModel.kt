/*
 *   Copyright 2024 Benoit LETONDOR
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
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.getListOfMonthsAvailableForUser
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseActivity.Companion.FROM_NOTIFICATION_EXTRA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyReportBaseViewModel @Inject constructor(
    private val parameters: Parameters,
    private val iab: Iab,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val fromNotification = savedStateHandle.get<Boolean>(FROM_NOTIFICATION_EXTRA) ?: false

    private var isUserPro = false

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private val stateMutableFlow = MutableStateFlow<State>(State.Loading)
    val stateFlow: Flow<State> = stateMutableFlow

    init {
        viewModelScope.launch {
            stateMutableFlow.value = State.Loading

            val months = withContext(Dispatchers.IO) {
                return@withContext parameters.getListOfMonthsAvailableForUser()
            }

            var currentMonthPosition = months.indexOf(YearMonth.now())
            if (currentMonthPosition == -1) {
                Logger.error("Error while getting current month position, returned -1", IllegalStateException("Current month not found in list of available months"))
                currentMonthPosition = months.size - 1
            }

            val selectedPosition = if( !fromNotification || months.size == 1) {
                MonthlyReportSelectedPosition(currentMonthPosition, months[currentMonthPosition], currentMonthPosition == months.size - 1)
            } else {
                MonthlyReportSelectedPosition(currentMonthPosition - 1, months[currentMonthPosition - 1], false)
            }

            stateMutableFlow.value = State.Loaded(months, selectedPosition)
        }

        viewModelScope.launch {
            iab.iabStatusFlow
                .map { it == PremiumCheckStatus.PRO_SUBSCRIBED }
                .distinctUntilChanged()
                .collect { isPro ->
                    isUserPro = isPro
                    eventMutableFlow.emit(Event.RefreshMenu)
                }
        }
    }

    fun shouldShowExportButton(): Boolean = isUserPro

    fun onPreviousMonthButtonClicked() {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        val position = loadedState.selectedPosition.position
        if (position > 0) {
            stateMutableFlow.value = loadedState.copy(
                selectedPosition = MonthlyReportSelectedPosition(position - 1, loadedState.months[position - 1], false)
            )
        }
    }

    fun onNextMonthButtonClicked() {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        val position = loadedState.selectedPosition.position
        if ( position < loadedState.months.size - 1 ) {
            stateMutableFlow.value = loadedState.copy(
                selectedPosition = MonthlyReportSelectedPosition(position + 1, loadedState.months[position + 1], loadedState.months.size == position + 2)
            )
        }
    }

    fun onPageSelected(position: Int) {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        stateMutableFlow.value = loadedState.copy(
            selectedPosition = MonthlyReportSelectedPosition(position, loadedState.months[position], loadedState.months.size == position + 1)
        )
    }

    fun onExportButtonClicked() {
        val loadedState = stateMutableFlow.value as? State.Loaded ?: return

        val selectedMonth = loadedState.selectedPosition.month
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenExport(selectedMonth))
        }
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(val months: List<YearMonth>, val selectedPosition: MonthlyReportSelectedPosition) : State()
    }

    sealed class Event {
        data object RefreshMenu : Event()
        data class OpenExport(val month: YearMonth) : Event()
    }
}

data class MonthlyReportSelectedPosition(val position: Int, val month: YearMonth, val latest: Boolean)