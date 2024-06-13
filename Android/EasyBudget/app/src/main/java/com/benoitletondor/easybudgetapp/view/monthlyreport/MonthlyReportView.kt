package com.benoitletondor.easybudgetapp.view.monthlyreport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.monthlyreport.subviews.EmptyView
import com.benoitletondor.easybudgetapp.view.monthlyreport.subviews.EntriesView
import com.benoitletondor.easybudgetapp.view.monthlyreport.subviews.ErrorView
import com.benoitletondor.easybudgetapp.view.monthlyreport.subviews.MonthsHeader
import com.benoitletondor.easybudgetapp.view.monthlyreport.subviews.RecapView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class MonthlyReportDestination(val fromNotification: Boolean)

@Composable
fun MonthlyReportView(
    viewModel: MonthlyReportViewModel,
    navigateUp: () -> Unit,
) {
    MonthlyReportView(
        navigateUp = navigateUp,
        shouldShowExportToCsvButtonFlow = viewModel.shouldShowExportToCsvButtonFlow,
        eventFlow = viewModel.eventFlow,
        stateFlow = viewModel.stateFlow,
        monthDataStateFlow = viewModel.monthDataStateFlow,
        userCurrencyStateFlow = viewModel.userCurrencyStateFlow,
        onExportToCsvButtonPressed = viewModel::onExportToCsvButtonPressed,
        onPreviousMonthClicked = viewModel::onPreviousMonthClicked,
        onNextMonthClicked = viewModel::onNextMonthClicked,
        onRetryLoadingMonthDataPressed = viewModel::onRetryLoadingMonthDataPressed,
    )
}

@Composable
private fun MonthlyReportView(
    navigateUp: () -> Unit,
    shouldShowExportToCsvButtonFlow: StateFlow<Boolean>,
    eventFlow: Flow<MonthlyReportViewModel.Event>,
    stateFlow: StateFlow<MonthlyReportViewModel.State>,
    monthDataStateFlow: StateFlow<MonthlyReportViewModel.MonthDataState>,
    userCurrencyStateFlow: StateFlow<Currency>,
    onExportToCsvButtonPressed: () -> Unit,
    onPreviousMonthClicked: () -> Unit,
    onNextMonthClicked: () -> Unit,
    onRetryLoadingMonthDataPressed: () -> Unit,
) {
    LaunchedEffect("eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                MonthlyReportViewModel.Event.OpenExportToCsvScreen -> TODO()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_monthly_report),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        actions = {
            val shouldShowExportToCsvButton by shouldShowExportToCsvButtonFlow.collectAsState()
            if (shouldShowExportToCsvButton) {
                IconButton(
                    onClick = onExportToCsvButtonPressed,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_upload_file_24),
                        contentDescription = stringResource(R.string.action_export),
                    )
                }
            }
        },
        content = { contentPaddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPaddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val state by stateFlow.collectAsState()
                when(val currentState = state) {
                    MonthlyReportViewModel.State.Loading -> LoadingView()
                    is MonthlyReportViewModel.State.Loaded -> {
                        MonthsHeader(
                            selectedPosition = currentState.selectedPosition,
                            onPreviousMonthClicked = onPreviousMonthClicked,
                            onNextMonthClicked = onNextMonthClicked,
                        )

                        MonthData(
                            monthDataStateFlow = monthDataStateFlow,
                            userCurrencyStateFlow = userCurrencyStateFlow,
                            onRetryButtonClicked = onRetryLoadingMonthDataPressed,
                        )
                    }
                }
            }
        }
    )
}



@Composable
private fun ColumnScope.MonthData(
    monthDataStateFlow: StateFlow<MonthlyReportViewModel.MonthDataState>,
    userCurrencyStateFlow: StateFlow<Currency>,
    onRetryButtonClicked: () -> Unit,
) {
    val monthDataState by monthDataStateFlow.collectAsState()
    when(val state = monthDataState) {
        MonthlyReportViewModel.MonthDataState.Loading -> LoadingView()
        is MonthlyReportViewModel.MonthDataState.Error -> ErrorView(
            error = state.error,
            onRetryButtonClicked = onRetryButtonClicked,
        )
        MonthlyReportViewModel.MonthDataState.Empty -> {
            RecapView(
                userCurrencyStateFlow = userCurrencyStateFlow,
                expensesAmount = 0.0,
                revenuesAmount = 0.0,
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.divider),
                thickness = 1.dp,
            )

            EmptyView()
        }
        is MonthlyReportViewModel.MonthDataState.Loaded -> {
            RecapView(
                userCurrencyStateFlow = userCurrencyStateFlow,
                expensesAmount = state.expensesAmount,
                revenuesAmount = state.revenuesAmount,
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.divider),
                thickness = 1.dp,
            )

            EntriesView(
                userCurrencyStateFlow = userCurrencyStateFlow,
                expenses = state.expenses,
                revenues = state.revenues,
            )
        }
    }
}