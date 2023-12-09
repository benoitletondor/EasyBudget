package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.benoitletondor.easybudgetapp.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarDatesView
import com.benoitletondor.easybudgetapp.view.main.account.calendar2.views.CalendarHeaderView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
) {
    CalendarView(
        displayedMonthFlow = viewModel.displayedMonthFlow,
        dataForMonthStateFlow = viewModel.dataForMonthState,
        onMonthChange = viewModel::onMonthChange,
        onRetryButtonClicked = viewModel::onRetryButtonClicked,
    )
}

@Composable
private fun CalendarView(
    displayedMonthFlow: StateFlow<YearMonth>,
    dataForMonthStateFlow: StateFlow<CalendarViewModel.DataForMonthState>,
    onMonthChange: (YearMonth) -> Unit,
    onRetryButtonClicked: () -> Unit,
) {
    val month by displayedMonthFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        CalendarHeaderView(
            month = month,
            canGoBack = true,
            canGoForward = true,
            onMonthChange = onMonthChange,
        )

        CalendarDatesView(
            month = month,
            dataForMonthStateFlow = dataForMonthStateFlow,
            onRetryButtonClicked = onRetryButtonClicked,
        )
    }
}



@Preview
@Composable
private fun CalendarViewPreview() {
    AppTheme {
        CalendarView(
            displayedMonthFlow = MutableStateFlow(YearMonth.now()),
            dataForMonthStateFlow = MutableStateFlow(CalendarViewModel.DataForMonthState.Loading),
            onMonthChange = {},
            onRetryButtonClicked = {},
        )
    }
}