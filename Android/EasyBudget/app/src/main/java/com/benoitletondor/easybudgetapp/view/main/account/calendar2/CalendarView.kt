package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.benoitletondor.easybudgetapp.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
) {
    CalendarView(
        displayedMonthFlow = viewModel.displayedMonthFlow,
    )
}

@Composable
private fun CalendarView(
    displayedMonthFlow: StateFlow<YearMonth>,
) {
    val month by displayedMonthFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        CalendarHeaderView(
            month = month,
            canGoBack = true,
            canGoForward = true,
        )

        CalendarDatesView(
            month = month,
        )
    }
}



@Preview
@Composable
private fun CalendarViewPreview() {
    AppTheme {
        CalendarView(
            displayedMonthFlow = MutableStateFlow(YearMonth.now()),
        )
    }
}