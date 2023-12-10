package com.benoitletondor.easybudgetapp.view.main.account.calendar2.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarDatesView(
    calendarState: CalendarState,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
    includeCheckedBalance: Boolean,
    selectedDateFlow: StateFlow<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClicked: (LocalDate) -> Unit,
) {
    val todayDate = remember { LocalDate.now() }

    HorizontalCalendar(
        state = calendarState,
        monthHeader = { month ->
            DaysOfWeekTitle(month = month)
        },
        monthBody = { calendarMonth, content ->
            val (state, setState) = remember { mutableStateOf<State>(State.NotAvailable) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect("InitialLoading") {
                withContext(Dispatchers.IO) {
                    loadData(
                        setState = setState,
                        calendarMonth = calendarMonth,
                        getDataForMonth = getDataForMonth,
                    )
                }
            }

            MonthBodyWithDataForMonth(maybeDataForMonth = state.maybeDataForMonth) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.alpha(if (state is State.Loaded) 1f else 0.1f),
                    ) {
                        content()
                    }

                    when(state) {
                        is State.Error -> {
                            ErrorView(
                                error = state.error,
                                onRetryButtonClicked = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        loadData(
                                            setState = setState,
                                            calendarMonth = calendarMonth,
                                            getDataForMonth = getDataForMonth,
                                        )
                                    }
                                },
                            )
                        }
                        is State.Loaded -> Unit
                        State.Loading, State.NotAvailable -> {
                            LoadingView()
                        }
                    }
                }
            }

        },
        dayContent = { calendarDay ->
            val maybeDataForMonth = LocalDataForMonth.current
            val maybeDataForDay = maybeDataForMonth?.daysData?.get(calendarDay.date)

            val selectedDate by selectedDateFlow.collectAsState()

            if (maybeDataForDay != null && maybeDataForDay.expenses.isNotEmpty()) {
                if (calendarDay.position == DayPosition.MonthDate) {
                    InCalendarWithBalanceDayView(
                        dayOfMonth = calendarDay.date.dayOfMonth,
                        balanceToDisplay = maybeDataForDay.balance,
                        displayUncheckedStyle = if (includeCheckedBalance) maybeDataForDay.expenses.any { !it.checked } else false,
                        selected = calendarDay.date == selectedDate,
                        today = calendarDay.date == todayDate,
                        onClick = {
                            onDateSelected(calendarDay.date)
                        },
                        onLongClick = {
                            onDateLongClicked(calendarDay.date)
                        },
                    )
                } else {
                    OffCalendarWithBalanceDayView(
                        dayOfMonth = calendarDay.date.dayOfMonth,
                        balanceToDisplay = maybeDataForDay.balance,
                        displayUncheckedStyle = if (includeCheckedBalance) maybeDataForDay.expenses.any { !it.checked } else false,
                        today = calendarDay.date == todayDate,
                        onClick = {
                            onDateSelected(calendarDay.date)
                        },
                        onLongClick = {
                            onDateLongClicked(calendarDay.date)
                        },
                    )
                }
            } else {
                if (calendarDay.position == DayPosition.MonthDate) {
                    InCalendarEmptyDayView(
                        dayOfMonth = calendarDay.date.dayOfMonth,
                        selected = calendarDay.date == selectedDate,
                        today = calendarDay.date == todayDate,
                        onClick = {
                            onDateSelected(calendarDay.date)
                        },
                        onLongClick = {
                            onDateLongClicked(calendarDay.date)
                        },
                    )
                } else {
                    OffCalendarEmptyDayView(
                        dayOfMonth = calendarDay.date.dayOfMonth,
                        today = calendarDay.date == todayDate,
                        onClick = {
                            onDateSelected(calendarDay.date)
                        },
                        onLongClick = {
                            onDateLongClicked(calendarDay.date)
                        },
                    )
                }
            }
        }
    )
}

@Composable
private fun DaysOfWeekTitle(
    month: CalendarMonth,
) {
    val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier
            .padding(top = 2.dp, bottom = 3.dp)
            .fillMaxWidth()
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
            )
        }
    }
}

private val LocalDataForMonth = compositionLocalOf<DataForMonth?> { null }

@Composable
private fun MonthBodyWithDataForMonth(
    maybeDataForMonth: DataForMonth?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDataForMonth provides maybeDataForMonth) {
        content()
    }
}

private sealed class State {
    abstract val maybeDataForMonth: DataForMonth?

    data object NotAvailable : State() {
        override val maybeDataForMonth = null
    }
    data object Loading : State() {
        override val maybeDataForMonth = null
    }
    data class Error(val error: Exception) : State() {
        override val maybeDataForMonth = null
    }
    data class Loaded(val dataForMonth: DataForMonth) : State() {
        override val maybeDataForMonth = dataForMonth
    }
}

private suspend fun loadData(
    setState: (State) -> Unit,
    calendarMonth: CalendarMonth,
    getDataForMonth: suspend (YearMonth) -> DataForMonth,
) {
    setState(State.Loading)
    setState(
        try {
            State.Loaded(dataForMonth = getDataForMonth(calendarMonth.yearMonth))
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Logger.error("Error while loading data for month: ${calendarMonth.yearMonth}", e)

            State.Error(e)
        }
    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "TODO", // TODO
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = error.localizedMessage ?: "Exception", // TODO
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onRetryButtonClicked,
        ) {
            Text(stringResource(R.string.manage_account_error_cta))
        }
    }
}