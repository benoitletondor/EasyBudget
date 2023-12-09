package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarDatesView
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarHeaderView
import com.kizitonwose.calendar.compose.rememberCalendarState
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
) {
    val state by viewModel.stateFlow.collectAsState()

    when(val currentState = state) {
        is CalendarViewModel.State.Error,
        CalendarViewModel.State.Loading -> Unit
        is CalendarViewModel.State.Loaded -> {
            CalendarView(
                includeCheckedBalance = currentState.includeCheckedBalance,
                getDataForMonth = currentState.getDataForMonth,
            )
        }
    }
}

@Composable
private fun CalendarView(
    includeCheckedBalance: Boolean,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coroutineScope = rememberCoroutineScope()
        val calendarState = rememberCalendarState(
            startMonth = YearMonth.now().minusMonths(100), // TODO
            endMonth = YearMonth.now().plusMonths(100), // TODO
            firstVisibleMonth = YearMonth.now(),
            firstDayOfWeek = DayOfWeek.MONDAY, // TODO
        )

        CalendarHeaderView(
            month = calendarState.firstVisibleMonth.yearMonth,
            canGoBack = true, // TODO
            canGoForward = true, // TODO
            onMonthChange = { yearMonth ->
                coroutineScope.launch {
                    calendarState.animateScrollToMonth(yearMonth)
                }
            },
        )

        CalendarDatesView(
            calendarState = calendarState,
            getDataForMonth = getDataForMonth,
            includeCheckedBalance = includeCheckedBalance,
        )
    }
}