/*
 *   Copyright 2025 Benoit Letondor
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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.toFormattedString
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.injection.requireDB
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@HiltViewModel(assistedFactory = MonthlyReportExportViewModelFactory::class)
class MonthlyReportExportViewModel @AssistedInject constructor(
    private val dbProvider: CurrentDBProvider,
    private val parameters: Parameters,
    @Assisted private val month: YearMonth,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private val retryMutableFlow = MutableSharedFlow<Unit>()

    val stateFlow: StateFlow<State> = flow<State> {
        val csvFile = withContext(Dispatchers.IO) {
            val expenses = dbProvider.requireDB.getExpensesForMonth(month)
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

        emit(State.Loaded(csvFile))
    }
        .retryWhen { cause, _ ->
            emit(State.Error(cause))
            Logger.error("Error creating month data CSV", cause)

            retryMutableFlow.first()
            emit(State.Loading)

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onCleared() {
        val tempFile = File(context.cacheDir, tempFileName(month))
        if (tempFile.exists()) {
            Logger.debug("Deleting temporary file: ${tempFile.path}")
            tempFile.delete()
        }

        super.onCleared()
    }

    fun onRetryButtonClicked() {
        viewModelScope.launch {
            retryMutableFlow.emit(Unit)
        }
    }

    fun onDownloadButtonClicked() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowShareCsv((stateFlow.value as State.Loaded).csvFile))
        }
    }

    fun onErrorOpeningShareCsv(e: Exception) {
        Logger.error("Error opening share CSV", e)
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowOpeningShareCsvError(e))
        }
    }

    fun onShareCsvFinished(success: Boolean) {
        if (success) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.Finish)
            }
        }
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(val csvFile: File) : State()
        data class Error(val error: Throwable) : State()
    }

    sealed class Event {
        data class ShowShareCsv(val csvFile: File) : Event()
        data class ShowOpeningShareCsvError(val error: Exception) : Event()
        data object Finish : Event()
    }
}

private fun tempFileName(month: YearMonth): String = "export_${month.year}_${month.monthValue}.csv"

@AssistedFactory
interface MonthlyReportExportViewModelFactory {
    fun create(month: YearMonth): MonthlyReportExportViewModel
}