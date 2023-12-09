package com.benoitletondor.easybudgetapp.view.main.account.calendar2.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.CalendarViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import java.time.DayOfWeek
import java.time.YearMonth

@Composable
fun CalendarDatesView(
    month: YearMonth,
    dataForMonthStateFlow: StateFlow<CalendarViewModel.DataForMonthState>,
    onRetryButtonClicked: () -> Unit,
) {
    val dataForMonthState by dataForMonthStateFlow.collectAsState()

    when(val state = dataForMonthState) {
        is CalendarViewModel.DataForMonthState.Error -> ErrorView(
            error = state.error,
            onRetryButtonClicked = onRetryButtonClicked,
        )
        is CalendarViewModel.DataForMonthState.Loaded -> LoadedView(
            month = month,
            dataForMonth = state.dataForMonth,
        )
        CalendarViewModel.DataForMonthState.Loading -> LoadingView()
    }
}

@Composable
private fun LoadedView(
    month: YearMonth,
    dataForMonth: DataForMonth,
) {
    val state = rememberCalendarState(
        startMonth = month,
        firstDayOfWeek = DayOfWeek.MONDAY, // TODO
    )

    HorizontalCalendar(
        state = state,
        dayContent = { calendarDay ->
            val dataForDay = dataForMonth.daysData[calendarDay.date]
            if (dataForDay == null) {
                Logger.error("No data for day: ${calendarDay.date}, month: $month")
                return@HorizontalCalendar Box {}
            }

            Day(
                dataForDay = dataForDay,
            )
        }
    )
}

@Composable
private fun Day(
    dataForDay: DataForDay,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f), // This is important for square sizing!
        contentAlignment = Alignment.Center
    ) {
        Text(text = dataForDay.day.dayOfMonth.toString())
    }
}

@Composable
private fun LoadingView() {
    Box {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ErrorView(
    error: Throwable,
    onRetryButtonClicked: () -> Unit,
) {

}