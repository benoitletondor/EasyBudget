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

package com.benoitletondor.easybudgetapp.view.recurringexpenseadd

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import kotlinx.coroutines.launch
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@HiltViewModel(assistedFactory = RecurringExpenseEditViewModelFactory::class)
class RecurringExpenseEditViewModel @AssistedInject constructor(
    private val parameters: Parameters,
    currentDBProvider: CurrentDBProvider,
    @Assisted private val editedExpense: Expense?,
    @Assisted date: LocalDate,
) : ViewModel() {
    private val dateMutableStateFlow = MutableStateFlow(date)
    private val amountMutableStateFlow = MutableStateFlow(editedExpense?.amount ?: 0.0)
    private val isRevenueMutableStateFlow = MutableStateFlow(editedExpense?.isRevenue() ?: false)
    private val titleMutableStateFlow = MutableStateFlow(editedExpense?.title ?: "")
    private val recurringExpenseTypeMutableStateFlow = MutableStateFlow(editedExpense?.associatedRecurringExpense?.recurringExpense?.type ?: RecurringExpenseType.MONTHLY)
    private val isSavingMutableStateFlow = MutableStateFlow(false)

    val stateFlow: StateFlow<State> = combine(
        dateMutableStateFlow,
        amountMutableStateFlow,
        isRevenueMutableStateFlow,
        titleMutableStateFlow,
        recurringExpenseTypeMutableStateFlow,
        isSavingMutableStateFlow,
    ) { date, amount, isRevenue, title, recurringExpenseType, isSaving ->
        val expense = Expense(
            id = editedExpense?.id,
            title = title,
            amount = if (isRevenue) -abs(amount) else abs(amount),
            date = date,
            checked = editedExpense?.checked ?: false,
            associatedRecurringExpense = editedExpense?.associatedRecurringExpense,
        )

        return@combine State(
            isEditing = editedExpense != null,
            isSaving = isSaving,
            recurringExpenseType = recurringExpenseType,
            isRevenue = isRevenue,
            expense = expense,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State(
            isEditing = editedExpense != null,
            isRevenue = editedExpense?.isRevenue() ?: false,
            isSaving = false,
            recurringExpenseType = recurringExpenseTypeMutableStateFlow.value,
            expense = Expense(
                id = editedExpense?.id,
                title = titleMutableStateFlow.value,
                amount = if (isRevenueMutableStateFlow.value) -abs(amountMutableStateFlow.value) else abs(
                    amountMutableStateFlow.value
                ),
                date = dateMutableStateFlow.value,
                checked = editedExpense?.checked ?: false,
                associatedRecurringExpense = editedExpense?.associatedRecurringExpense,
            )
        )
    )

    val userCurrencyFlow = parameters.watchUserCurrency()

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private lateinit var db: DB

    init {
        val currentDb = currentDBProvider.activeDB
        if (currentDb == null) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.UnableToLoadDB)
            }
        } else {
            db = currentDb
        }
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        isRevenueMutableStateFlow.value = isRevenue
    }

    fun onDateSelected(utcTimestamp: Long?) {
        if (utcTimestamp != null) {
            dateMutableStateFlow.value = Instant.ofEpochMilli(utcTimestamp)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
        }
    }

    fun onDateClicked() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowDatePicker(dateMutableStateFlow.value))
        }
    }

    fun onRecurringExpenseTypeChanged(recurringExpenseType: RecurringExpenseType) {
        recurringExpenseTypeMutableStateFlow.value = recurringExpenseType
    }

    fun onAmountChanged(amount: String) {
        amountMutableStateFlow.value = amount.toDoubleOrNull() ?: 0.0
    }

    fun onTitleChanged(title: String) {
        titleMutableStateFlow.value = title
    }

    fun onSave() {
        var isInError = false
        if (titleMutableStateFlow.value.isEmpty()) {
            isInError = true
            viewModelScope.launch {
                eventMutableFlow.emit(Event.EmptyTitleError)
            }
        }

        if (amountMutableStateFlow.value == 0.0) {
            isInError = true
            viewModelScope.launch {
                eventMutableFlow.emit(Event.EmptyAmountError)
            }
        }

        if (isInError) {
            return
        }

        val date = dateMutableStateFlow.value
        val dateOfInstallation = parameters.getInitDate() ?: LocalDate.now()
        if( date.isBefore(dateOfInstallation) ) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ExpenseAddBeforeInitDateError)
            }

            return
        }

        doSaveExpense(stateFlow.value.expense, stateFlow.value.recurringExpenseType)
    }

    fun onAddExpenseBeforeInitDateConfirmed() {
        doSaveExpense(stateFlow.value.expense, stateFlow.value.recurringExpenseType)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(
        expense: Expense,
        recurringExpenseType: RecurringExpenseType,
    ) {
        isSavingMutableStateFlow.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (editedExpense == null) {
                        db.persistRecurringExpense(
                            RecurringExpense(
                                expense.title,
                                expense.amount,
                                expense.date,
                                recurringExpenseType
                            )
                        )
                        return@withContext true
                    } else {
                        val recurringExpense =
                            editedExpense.associatedRecurringExpense!!.recurringExpense
                        db.updateRecurringExpenseAfterDate(
                            recurringExpense.copy(
                                modified = true,
                                type = recurringExpenseType,
                                recurringDate = expense.date,
                                title = expense.title,
                                amount = expense.amount,
                            ),
                            editedExpense.date,
                        )
                    }
                }

                eventMutableFlow.emit(Event.Finish)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error persisting recurring expense", e)
                eventMutableFlow.emit(Event.ErrorPersistingExpense(e))
            } finally {
                isSavingMutableStateFlow.value = false
            }
        }
    }

    sealed class Event {
        data object Finish : Event()
        data object UnableToLoadDB : Event()
        data object EmptyTitleError : Event()
        data object EmptyAmountError : Event()
        data object ExpenseAddBeforeInitDateError : Event()
        data class ErrorPersistingExpense(val error: Throwable) : Event()
        data class ShowDatePicker(val date: LocalDate) : Event()
    }

    @Immutable
    data class State(
        val isEditing: Boolean,
        val isSaving: Boolean,
        val recurringExpenseType: RecurringExpenseType,
        val expense: Expense,
        val isRevenue: Boolean,
    )
}

@AssistedFactory
interface RecurringExpenseEditViewModelFactory {
    fun create(
        editedExpense: Expense?,
        date: LocalDate,
    ): RecurringExpenseEditViewModel
}