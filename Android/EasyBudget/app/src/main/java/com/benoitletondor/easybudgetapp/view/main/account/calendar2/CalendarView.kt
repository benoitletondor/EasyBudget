package com.benoitletondor.easybudgetapp.view.main.account.calendar2

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
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarDatesView
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarHeaderView
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarView(
    parameters: Parameters,
    dbAvailableFlow: StateFlow<AccountViewModel.DBState>,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    selectedDateFlow: StateFlow<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
) {
    val dbState by dbAvailableFlow.collectAsState()

    when(val currentDbState = dbState) {
        is AccountViewModel.DBState.Error,
        AccountViewModel.DBState.Loading -> Unit
        is AccountViewModel.DBState.Loaded -> CalendarView(
            appInitDate = parameters.getInitDate() ?: LocalDate.now(),
            firstDayOfWeek = parameters.getFirstDayOfWeek(),
            includeCheckedBalanceFlow = includeCheckedBalanceFlow,
            getDataForMonth = { yearMonth ->
                currentDbState.db.getDataForMonth(yearMonth, includeCheckedBalanceFlow.value)
            },
            selectedDateFlow = selectedDateFlow,
            onDateSelected = onDateSelected,
            onDateLongClicked = onDateLongClicked,
        )
    }
}

@Composable
private fun CalendarView(
    appInitDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    includeCheckedBalanceFlow: StateFlow<Boolean>,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
    selectedDateFlow: StateFlow<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coroutineScope = rememberCoroutineScope()
        val calendarState = rememberCalendarState(
            startMonth = appInitDate.computeCalendarMinDateFromInitDate().yearMonth,
            endMonth = YearMonth.now().plusYears(10),
            firstVisibleMonth = YearMonth.now(),
            firstDayOfWeek = firstDayOfWeek,
        )

        var canGoBack by remember { mutableStateOf(true) }
        var canGoForward by remember { mutableStateOf(true) }

        LaunchedEffect(calendarState.firstVisibleMonth.yearMonth) {
            canGoBack = calendarState.firstVisibleMonth.yearMonth.isAfter(calendarState.startMonth)
            canGoForward = calendarState.firstVisibleMonth.yearMonth.isBefore(calendarState.endMonth)
        }

        LaunchedEffect("selectedDateChange") {
            launch {
                selectedDateFlow.collect { date ->
                    if (date.yearMonth !== calendarState.firstVisibleMonth.yearMonth) {
                        calendarState.animateScrollToMonth(date.yearMonth)
                    }
                }
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
            getDataForMonth = getDataForMonth,
            includeCheckedBalance = includeCheckedBalance,
            selectedDateFlow = selectedDateFlow,
            onDateSelected = onDateSelected,
            onDateLongClicked = onDateLongClicked,
        )
    }
}