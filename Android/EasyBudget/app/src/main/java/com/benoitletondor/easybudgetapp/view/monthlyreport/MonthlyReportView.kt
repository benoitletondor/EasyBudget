package com.benoitletondor.easybudgetapp.view.monthlyreport

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

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
        onExportToCsvButtonPressed = viewModel::onExportToCsvButtonPressed,
    )
}

@Composable
private fun MonthlyReportView(
    navigateUp: () -> Unit,
    shouldShowExportToCsvButtonFlow: StateFlow<Boolean>,
    eventFlow: Flow<MonthlyReportViewModel.Event>,
    onExportToCsvButtonPressed: () -> Unit,
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

        }
    )
}