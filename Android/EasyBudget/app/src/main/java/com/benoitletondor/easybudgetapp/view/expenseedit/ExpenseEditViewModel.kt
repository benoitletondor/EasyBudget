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

package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.combine
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
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
    currentDBProvider: CurrentDBProvider,
    @Assisted editedExpense: Expense?,
    @Assisted date: LocalDate,
) : ViewModel() {
    private val editedExpenseMutableStateFlow = MutableStateFlow(editedExpense)
    private val dateMutableStateFlow = MutableStateFlow(date)
    private val amountMutableStateFlow = MutableStateFlow(editedExpense?.amount ?: 0.0)
    private val isRevenueMutableStateFlow = MutableStateFlow(editedExpense?.isRevenue() ?: false)
    private val titleMutableStateFlow = MutableStateFlow(editedExpense?.title ?: "")
    private val isSavingMutableStateFlow = MutableStateFlow(false)

    val userCurrencyFlow = parameters.watchUserCurrency()

    val stateFlow: StateFlow<State> = combine(
        editedExpenseMutableStateFlow,
        dateMutableStateFlow,
        amountMutableStateFlow,
        isRevenueMutableStateFlow,
        titleMutableStateFlow,
        isSavingMutableStateFlow,
    ) { maybeEditedExpense, date, amount, isRevenue, title, isSaving ->
        val expense = Expense(
            id = maybeEditedExpense?.id,
            title = title,
            amount = if (isRevenue) -abs(amount) else abs(amount),
            date = date,
            checked = maybeEditedExpense?.checked ?: false,
            associatedRecurringExpense = maybeEditedExpense?.associatedRecurringExpense,
        )

        if (isSaving) {
            return@combine State.Saving(
                isEditing = maybeEditedExpense != null,
                isRevenue = isRevenue,
                expense = expense,
            )
        }

        return@combine State.Ready(
            isEditing = maybeEditedExpense != null,
            isRevenue = isRevenue,
            expense = expense,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Ready(
        isEditing = editedExpense != null,
        isRevenue = editedExpense?.isRevenue() ?: false,
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

    fun onAmountChanged(amount: String) {
        amountMutableStateFlow.value = amount.toDoubleOrNull() ?: 0.0 // FIXME
    }

    fun onTitleChanged(title: String) {
        titleMutableStateFlow.value = title
    }

    fun onSave() {
        val date = dateMutableStateFlow.value
        val dateOfInstallation = parameters.getInitDate() ?: LocalDate.now()
        if( date.isBefore(dateOfInstallation) ) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ExpenseAddBeforeInitDateError)
            }

            return
        }

        if (titleMutableStateFlow.value.isEmpty()) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.EmptyTitleError)
            }

            return
        }

        doSaveExpense((stateFlow.value as State.Ready).expense)
    }

    fun onAddExpenseBeforeInitDateConfirmed() {
        doSaveExpense((stateFlow.value as State.Ready).expense)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(expense: Expense) {
        viewModelScope.launch {
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
        data object ExpenseAddBeforeInitDateError : Event()
        data class ErrorPersistingExpense(val error: Throwable) : Event()
        data class ShowDatePicker(val date: LocalDate) : Event()
    }

    sealed interface State {
        val isEditing: Boolean
        val expense: Expense
        val isRevenue: Boolean

        @Immutable
        data class Ready(override val isEditing: Boolean, override val isRevenue: Boolean, override val expense: Expense) : State
        @Immutable
        data class Saving(override val isEditing: Boolean, override val isRevenue: Boolean, override val expense: Expense) : State
    }
}

@AssistedFactory
interface ExpenseEditViewModelFactory {
    fun create(
        date: LocalDate,
        editedExpense: Expense?,
    ): ExpenseEditViewModel
}