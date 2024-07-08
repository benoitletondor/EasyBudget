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
package com.benoitletondor.easybudgetapp.view.monthlyreport.export.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppTheme
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.view.monthlyreport.export.MonthlyReportExportViewModel
import kotlinx.coroutines.flow.StateFlow
import java.lang.IllegalArgumentException

@Composable
fun ContentView(
    stateFlow: StateFlow<MonthlyReportExportViewModel.State>,
    onRetryButtonClicked: () -> Unit,
    onDownloadButtonClicked: () -> Unit,
) {
    val state by stateFlow.collectAsState()

    when(val currentState = state) {
        is MonthlyReportExportViewModel.State.Error -> ErrorView(
            error = currentState.error,
            onRetryButtonClicked = onRetryButtonClicked,
        )
        is MonthlyReportExportViewModel.State.Loaded -> LoadedView(
            onDownloadButtonClicked = onDownloadButtonClicked,
        )
        MonthlyReportExportViewModel.State.Loading -> LoadingView()
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
            text = stringResource(id = R.string.monthly_report_data_loading_error_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.monthly_report_data_loading_error_description, error.localizedMessage ?: "No error message"),
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onRetryButtonClicked,
        ) {
            Text(stringResource(R.string.monthly_report_data_loading_error_cta))
        }
    }
}

@Composable
private fun LoadedView(
    onDownloadButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.monthly_report_export_data_loaded_title),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onDownloadButtonClicked,
        ) {
            Text(
                text = stringResource(id = R.string.monthly_report_export_data_loaded_cta),
            )
        }
    }
}

@Composable
@Preview(name = "Loading preview", showSystemUi = true)
private fun LoadingPreview() {
    AppTheme {
        LoadingView()
    }
}

@Composable
@Preview(name = "Error preview", showSystemUi = true)
private fun ErrorPreview() {
    AppTheme {
        ErrorView(
            error = IllegalArgumentException("An error occurred"),
            onRetryButtonClicked = {},
        )
    }
}

@Composable
@Preview(name = "Success preview", showSystemUi = true)
private fun SuccessPreview() {
    AppTheme {
        LoadedView(
            onDownloadButtonClicked = {},
        )
    }
}