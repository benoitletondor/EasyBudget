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
package com.benoitletondor.easybudgetapp.view.monthlyreport.export

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.serialization.SerializedYearMonth
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.monthlyreport.export.subviews.ContentView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyReportExportDestination(val month: SerializedYearMonth)

@Composable
fun MonthlyReportExportView(
    viewModel: MonthlyReportExportViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    MonthlyReportExportView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        onRetryButtonClicked = viewModel::onRetryButtonClicked,
        onDownloadButtonClicked = viewModel::onDownloadButtonClicked,
        onErrorOpeningShareCsv = viewModel::onErrorOpeningShareCsv,
        onShareCsvFinished = viewModel::onShareCsvFinished,
        finish = finish,
    )
}

@Composable
private fun MonthlyReportExportView(
    stateFlow: StateFlow<MonthlyReportExportViewModel.State>,
    eventFlow: Flow<MonthlyReportExportViewModel.Event>,
    navigateUp: () -> Unit,
    onRetryButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit,
    onErrorOpeningShareCsv: (Exception) -> Unit,
    onShareCsvFinished: (success: Boolean) -> Unit,
    finish: () -> Unit,
) {
    val context = LocalContext.current

    val shareCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onShareCsvFinished(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is MonthlyReportExportViewModel.Event.ShowShareCsv -> {
                    try {
                        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", event.csvFile)

                        val shareIntent = Intent().apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Allow external app to open the file
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "text/csv"
                        }

                        shareCsvLauncher.launch(Intent.createChooser(shareIntent, null))
                    } catch (e: Exception) {
                        onErrorOpeningShareCsv(e)
                    }
                }
                is MonthlyReportExportViewModel.Event.ShowOpeningShareCsvError -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.monthly_report_data_open_error_title)
                        .setMessage(R.string.monthly_report_data_open_error_description)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
                MonthlyReportExportViewModel.Event.Finish -> finish()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_monthly_report_export),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding),
            ) {
                ContentView(
                    stateFlow = stateFlow,
                    onRetryButtonClicked = onRetryButtonClicked,
                    onDownloadButtonClicked = onDownloadButtonClicked,
                )
            }
        },
    )
}