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

package com.benoitletondor.easybudgetapp.view.main.account.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.helper.computeCalendarMinDateFromInitDate
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.benoitletondor.easybudgetapp.parameters.watchFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.watchLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.benoitletondor.easybudgetapp.view.main.account.calendar.views.CalendarDatesView
import com.benoitletondor.easybudgetapp.view.main.account.calendar.views.CalendarHeaderView
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarView(
    parameters: Parameters,
    dbAvailableFlow: StateFlow<AccountViewModel.DBState>,
    forceRefreshDataFlow: Flow<Unit>,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    selectedDateFlow: StateFlow<LocalDate>,
    onMonthChanged: (YearMonth) -> Unit,
    goBackToCurrentMonthEventFlow: Flow<Unit>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
) {
    val dbState by dbAvailableFlow.collectAsState()

    when(val currentDbState = dbState) {
        is AccountViewModel.DBState.Error,
        AccountViewModel.DBState.Loading -> Unit
        is AccountViewModel.DBState.Loaded -> CalendarView(
            appInitDate = parameters.getInitDate() ?: LocalDate.now(),
            forceRefreshDataFlow = forceRefreshDataFlow,
            firstDayOfWeekFlow = parameters.watchFirstDayOfWeek(),
            includeCheckedBalanceFlow = includeCheckedBalanceFlow,
            getDataForMonth = currentDbState.db::getDataForMonth,
            selectedDateFlow = selectedDateFlow,
            lowMoneyAmountWarningFlow = parameters.watchLowMoneyWarningAmount(),
            onMonthChanged = onMonthChanged,
            goBackToCurrentMonthEventFlow = goBackToCurrentMonthEventFlow,
            onDateSelected = onDateSelected,
            onDateLongClicked = onDateLongClicked,
        )
    }
}

@Composable
private fun CalendarView(
    appInitDate: LocalDate,
    forceRefreshDataFlow: Flow<Unit>,
    firstDayOfWeekFlow: StateFlow<DayOfWeek>,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
    selectedDateFlow: StateFlow<LocalDate>,
    lowMoneyAmountWarningFlow: StateFlow<Int>,
    onMonthChanged: (YearMonth) -> Unit,
    goBackToCurrentMonthEventFlow: Flow<Unit>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coroutineScope = rememberCoroutineScope()
        val firstDayOfWeek = remember { firstDayOfWeekFlow.value }
        val firstVisibleMonth = remember { selectedDateFlow.value.yearMonth }
        val startMonth = remember { appInitDate.computeCalendarMinDateFromInitDate().yearMonth }
        val endMonth = remember { YearMonth.now().plusYears(10) }

        val calendarState = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = firstVisibleMonth,
            firstDayOfWeek = firstDayOfWeek,
        )

        var canGoBack by remember { mutableStateOf(true) }
        var canGoForward by remember { mutableStateOf(true) }

        LaunchedEffect(calendarState.firstVisibleMonth.yearMonth) {
            canGoBack = calendarState.firstVisibleMonth.yearMonth.isAfter(calendarState.startMonth)
            canGoForward = calendarState.firstVisibleMonth.yearMonth.isBefore(calendarState.endMonth)

            onMonthChanged(calendarState.firstVisibleMonth.yearMonth)
        }

        LaunchedEffect("FirstDayOfWeekChange") {
            launchCollect(firstDayOfWeekFlow) {
                calendarState.firstDayOfWeek = it
            }
        }

        LaunchedEffect("selectedDateChange") {
            launchCollect(selectedDateFlow) { date ->
                if (date.yearMonth !== calendarState.firstVisibleMonth.yearMonth) {
                    calendarState.animateScrollToMonth(date.yearMonth)
                }
            }
        }

        LaunchedEffect("goBackToCurrentMonthListener") {
            launchCollect(goBackToCurrentMonthEventFlow) {
                calendarState.animateScrollToMonth(YearMonth.now())
            }
        }

        CalendarHeaderView(
            month = calendarState.firstVisibleMonth.yearMonth,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onMonthChange = { yearMonth ->
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(yearMonth)
                }
            },
        )

        val includeCheckedBalance by includeCheckedBalanceFlow.collectAsState()
        CalendarDatesView(
            calendarState = calendarState,
            forceRefreshDataFlow = forceRefreshDataFlow,
            getDataForMonth = getDataForMonth,
            includeCheckedBalance = includeCheckedBalance,
            selectedDateFlow = selectedDateFlow,
            lowMoneyAmountWarningFlow = lowMoneyAmountWarningFlow,
            onDateSelected = onDateSelected,
            onDateLongClicked = onDateLongClicked,
        )
    }
}