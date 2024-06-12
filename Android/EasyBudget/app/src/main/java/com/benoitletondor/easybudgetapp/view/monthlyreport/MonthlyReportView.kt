package com.benoitletondor.easybudgetapp.view.monthlyreport

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.getMonthTitle
import com.benoitletondor.easybudgetapp.helper.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

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

@SuppressLint("PrivateResource")
@Composable
private fun MonthsHeader(
    selectedPosition: MonthlyReportSelectedPosition,
    onPreviousMonthClicked: () -> Unit,
    onNextMonthClicked: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPreviousMonthClicked,
            enabled = !selectedPosition.first,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(R.color.monthly_report_month_switch_button),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_navigate_before_24),
                contentDescription = stringResource(androidx.compose.material3.R.string.m3c_date_picker_switch_to_previous_month),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = selectedPosition.month.getMonthTitle(context).uppercase(Locale.getDefault()),
            fontSize = 21.sp,
            color = colorResource(R.color.primary_text),
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNextMonthClicked,
            enabled = !selectedPosition.last,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(R.color.monthly_report_month_switch_button),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_navigate_next_24),
                contentDescription = stringResource(androidx.compose.material3.R.string.m3c_date_picker_switch_to_next_month),
            )
        }
    }
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

            EntriesView()
        }
    }
}

@Composable
private fun ColumnScope.RecapView(
    userCurrencyStateFlow: StateFlow<Currency>,
    expensesAmount: Double,
    revenuesAmount: Double,
) {
    val currency by userCurrencyStateFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ){
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.revenues_total),
                    color = colorResource(R.color.monthly_report_categories_title),
                    fontSize = 18.sp,
                )

                Text(
                    text = CurrencyHelper.getFormattedCurrencyString(currency, revenuesAmount),
                    color = colorResource(R.color.monthly_report_categories_value),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.expenses_total),
                    color = colorResource(R.color.monthly_report_categories_title),
                    fontSize = 18.sp,
                )

                Text(
                    text = CurrencyHelper.getFormattedCurrencyString(currency, expensesAmount),
                    color = colorResource(R.color.monthly_report_categories_value),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.balance),
                color = colorResource(R.color.monthly_report_categories_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            val balance = revenuesAmount - expensesAmount

            Text(
                text = CurrencyHelper.getFormattedCurrencyString(currency, balance),
                color = colorResource(if (balance >= 0) R.color.budget_green else R.color.budget_red),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ColumnScope.EntriesView() {

}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_date_grey_48dp),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.monthly_report_no_entries_placeholder),
            color = colorResource(R.color.placeholder_text),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
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
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.calendar_month_loading_error_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.account_error_loading_message, error.localizedMessage ?: "No error message"),
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