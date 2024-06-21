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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

@HiltViewModel(assistedFactory = ExpenseEditViewModelFactory::class)
class ExpenseEditViewModel @AssistedInject constructor(
    private val parameters: Parameters,
    currentDBProvider: CurrentDBProvider,
    @Assisted editedExpense: Expense?,
    @Assisted date: LocalDate,
) : ViewModel() {

    private val expenseMutableStateFlow = MutableStateFlow(
        editedExpense?.copy(date = date) ?: Expense("", 0.0, date, false)
    )
    val expenseStateFlow: StateFlow<Expense> = expenseMutableStateFlow

    private val isEditingMutableStateFlow = MutableStateFlow(editedExpense != null)
    val isEditingStateFlow: StateFlow<Boolean> = isEditingMutableStateFlow

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
        expenseMutableStateFlow.value = expenseMutableStateFlow.value.copy(
            amount = if (isRevenue) -abs(expenseMutableStateFlow.value.amount) else abs(expenseMutableStateFlow.value.amount)
        )
    }

    fun onDateChanged(date: LocalDate) {
        expenseMutableStateFlow.value = expenseMutableStateFlow.value.copy(date = date)
    }

    fun onAmountChanged(amount: Double) {
        expenseMutableStateFlow.value = expenseMutableStateFlow.value.copy(
            amount = if (expenseMutableStateFlow.value.amount < 0) -abs(amount) else abs(amount)
        )
    }

    fun onSave() {
        val date = expenseMutableStateFlow.value.date
        val dateOfInstallation = parameters.getInitDate() ?: LocalDate.now()
        if( date.isBefore(dateOfInstallation) ) {
            viewModelScope.launch {
                eventMutableFlow.emit(Event.ExpenseAddBeforeInitDateError)
            }

            return
        }

        doSaveExpense(expenseMutableStateFlow.value)
    }

    fun onAddExpenseBeforeInitDateConfirmed() {
        doSaveExpense(expenseMutableStateFlow.value)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(expense: Expense) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                db.persistExpense(expense)

                withContext(Dispatchers.Main) {
                    eventMutableFlow.emit(Event.Finish)
                }
            }
        }
    }

    sealed class Event {
        data object Finish : Event()
        data object UnableToLoadDB : Event()
        data object ExpenseAddBeforeInitDateError : Event()
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double)

@AssistedFactory
interface ExpenseEditViewModelFactory {
    fun create(
        date: LocalDate,
        editedExpense: Expense?,
    ): ExpenseEditViewModel
}