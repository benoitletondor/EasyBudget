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
package com.benoitletondor.easybudgetapp.view.report.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityMonthlyReportExportBinding
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.toFormattedString
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.benoitletondor.easybudgetapp.view.report.export.ExportReportActivity.Companion.REQUEST_CODE_SHARE_CSV
import com.benoitletondor.easybudgetapp.view.report.export.ExportReportActivity.Companion.tempFileName
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.IllegalArgumentException
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ExportReportActivity: BaseActivity<ActivityMonthlyReportExportBinding>() {
    private lateinit var month: YearMonth
    @Inject
    lateinit var parameters: Parameters

    override fun createBinding(): ActivityMonthlyReportExportBinding = ActivityMonthlyReportExportBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        month = intent.getSerializableExtra(EXTRA_MONTH) as? YearMonth ?: throw IllegalArgumentException("Missing month extra")
        val currentDb = AccountViewModel.getCurrentDB()
        if (currentDb == null) {
            Logger.error("Unable to get current DB in ExportReportActivity", Exception("No DB found"))
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.exportComposeView.setContent {
            AppTheme {
                ExportReportScreen(
                    db = currentDb,
                    parameters = parameters,
                    month = month,
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SHARE_CSV && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    override fun onDestroy() {
        val tempFile = File(cacheDir, tempFileName(month))
        if (tempFile.exists()) {
            Logger.debug("Deleting temporary file: ${tempFile.path}")
            tempFile.delete()
        }

        super.onDestroy()
    }

    companion object {
        private const val EXTRA_MONTH = "extra_month"
        const val REQUEST_CODE_SHARE_CSV = 488230

        fun createIntent(context: Context, month: YearMonth): Intent = Intent(context, ExportReportActivity::class.java).apply {
            putExtra(EXTRA_MONTH, month)
        }

        fun tempFileName(month: YearMonth): String = "export_${month.year}_${month.monthValue}.csv"
    }
}

private sealed class State {
    data object Loading : State()
    data class Loaded(val csvFile: File) : State()
    data class Error(val exception: Exception) : State()
}

@Composable
private fun ExportReportScreen(
    db: DB,
    parameters: Parameters,
    month: YearMonth,
) {
    var retryLoadingState by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<State>(State.Loading) }

    val context = LocalContext.current
    val activity = context as Activity

    LaunchedEffect(month, retryLoadingState) {
        state = State.Loading
        state = try {
            val csvFile = withContext(Dispatchers.IO) {
                val expenses = db.getExpensesForMonth(month)
                val file = File(context.cacheDir, tempFileName(month))
                val dateFormatter = DateTimeFormatter.ISO_DATE

                Logger.debug("Creating temporary file: ${file.path}")
                csvWriter().openAsync(file) {
                    writeRow(listOf(
                        context.getString(R.string.monthly_report_data_date_row),
                        context.getString(R.string.monthly_report_data_title_row),
                        context.getString(R.string.monthly_report_data_amount_row),
                        context.getString(R.string.monthly_report_data_recurring_row),
                        context.getString(R.string.monthly_report_data_checked_row),
                    ))
                    for(expense in expenses) {
                        writeRow(listOf(
                            dateFormatter.format(expense.date),
                            expense.title,
                            CurrencyHelper.getFormattedCurrencyString(parameters, -expense.amount),
                            expense.associatedRecurringExpense?.recurringExpense?.type?.toFormattedString(context) ?: "",
                            if (expense.checked) "X" else "",
                        ))
                    }
                }

                return@withContext file
            }

            State.Loaded(csvFile)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            State.Error(e)
        }
    }

    when(val currentState = state) {
        is State.Error -> ErrorView(
            exception = currentState.exception,
            onRetryButtonClicked = { retryLoadingState++ },
        )
        is State.Loaded -> LoadedView(
            onDownloadButtonClicked = {
                try {
                    val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", currentState.csvFile)

                    val shareIntent = Intent().apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Allow external app to open the file
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "text/csv"
                    }

                    activity.startActivityForResult(Intent.createChooser(shareIntent, null), REQUEST_CODE_SHARE_CSV)
                } catch (e: Exception) {
                    Logger.error("Error while opening CSV file", e)
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.monthly_report_data_open_error_title)
                        .setMessage(R.string.monthly_report_data_open_error_description)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
        )
        State.Loading -> LoadingView()
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ErrorView(
    exception: Exception,
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
            text = stringResource(id = R.string.monthly_report_data_loading_error_description, exception.localizedMessage ?: "No error message"),
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
            exception = IllegalArgumentException("An error occurred"),
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