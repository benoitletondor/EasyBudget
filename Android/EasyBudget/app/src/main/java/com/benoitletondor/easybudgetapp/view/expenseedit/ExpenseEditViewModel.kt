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

package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@HiltViewModel(assistedFactory = ExpenseEditViewModelFactory::class)
class ExpenseEditViewModel @AssistedInject constructor(
    private val parameters: Parameters,
    private val currentDBProvider: CurrentDBProvider,
    @Assisted private val editedExpense: Expense?,
    @Assisted date: LocalDate,
) : ViewModel() {
    private val dateMutableStateFlow = MutableStateFlow(date)
    private val amountMutableStateFlow = MutableStateFlow(editedExpense?.amount ?: 0.0)
    private val isRevenueMutableStateFlow = MutableStateFlow(editedExpense?.isRevenue() ?: false)
    private val titleMutableStateFlow = MutableStateFlow(editedExpense?.title ?: "")
    private val isSavingMutableStateFlow = MutableStateFlow(false)

    val userCurrencyFlow = parameters.watchUserCurrency()

    val stateFlow: StateFlow<State> = combine(
        dateMutableStateFlow,
        amountMutableStateFlow,
        isRevenueMutableStateFlow,
        titleMutableStateFlow,
        isSavingMutableStateFlow,
    ) { date, amount, isRevenue, title, isSaving ->
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
            isRevenue = isRevenue,
            expense = expense,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State(
        isEditing = editedExpense != null,
        isRevenue = editedExpense?.isRevenue() ?: false,
        isSaving = false,
        expense = Expense(
            id = editedExpense?.id,
            title = titleMutableStateFlow.value,
            amount = if (isRevenueMutableStateFlow.value) -abs(amountMutableStateFlow.value) else abs(amountMutableStateFlow.value),
            date = dateMutableStateFlow.value,
            checked = editedExpense?.checked ?: false,
            associatedRecurringExpense = editedExpense?.associatedRecurringExpense,
        )
    ))

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

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

        doSaveExpense(stateFlow.value.expense)
    }

    fun onAddExpenseBeforeInitDateConfirmed() {
        doSaveExpense(stateFlow.value.expense)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(expense: Expense) {
        viewModelScope.launch {
            val db = currentDBProvider.activeDB ?: run {
                eventMutableFlow.emit(Event.UnableToLoadDB)
                return@launch
            }

            isSavingMutableStateFlow.value = true

            try {
                withContext(Dispatchers.IO) {
                    db.persistExpense(expense)
                }

                eventMutableFlow.emit(Event.Finish)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e

                Logger.error("Error while persisting expense", e)
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
        val expense: Expense,
        val isRevenue: Boolean,
    )
}

@AssistedFactory
interface ExpenseEditViewModelFactory {
    fun create(
        date: LocalDate,
        editedExpense: Expense?,
    ): ExpenseEditViewModel
}