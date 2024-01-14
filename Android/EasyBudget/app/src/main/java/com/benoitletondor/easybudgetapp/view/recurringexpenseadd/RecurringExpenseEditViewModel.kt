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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import kotlinx.coroutines.launch
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecurringExpenseEditViewModel @Inject constructor(
    private val parameters: Parameters,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private val editedExpense: Expense? = savedStateHandle[RecurringExpenseEditActivity.ARG_EXPENSE]

    private val expenseDateMutableStateFlow = MutableStateFlow(LocalDate.ofEpochDay(
        savedStateHandle[RecurringExpenseEditActivity.ARG_START_DATE] ?: throw IllegalStateException("No ARG_START_DATE arg")))
    val expenseDateFlow: Flow<LocalDate> = expenseDateMutableStateFlow

    private val editTypeMutableStateFlow = MutableStateFlow(ExpenseEditType(
        editedExpense?.isRevenue() ?: false,
        editedExpense != null
    ))
    val editTypeFlow: Flow<ExpenseEditType> = editTypeMutableStateFlow

    val existingExpenseData = editedExpense?.let { expense ->
        ExistingExpenseData(expense.title, expense.amount, expense.associatedRecurringExpense!!.recurringExpense.type)
    }

    private val savingStateMutableStateFlow: MutableStateFlow<SavingState> = MutableStateFlow(SavingState.Idle)
    val savingStateFlow: Flow<SavingState> = savingStateMutableStateFlow

    private val expenseAddBeforeInitDateErrorMutableFlow = MutableLiveFlow<Unit>()
    val expenseAddBeforeInitDateEventFlow: Flow<Unit> = expenseAddBeforeInitDateErrorMutableFlow

    private val unableToLoadDBEventMutableFlow = MutableLiveFlow<Unit>()
    val unableToLoadDBEventFlow: Flow<Unit> = unableToLoadDBEventMutableFlow

    private val finishMutableFlow = MutableLiveFlow<Unit>()
    val finishFlow: Flow<Unit> = finishMutableFlow

    private val errorMutableFlow = MutableLiveFlow<Unit>()
    val errorFlow: Flow<Unit> = errorMutableFlow

    private lateinit var db: DB

    init {
        val currentDb = AccountViewModel.getCurrentDB()
        if (currentDb == null) {
            viewModelScope.launch {
                unableToLoadDBEventMutableFlow.emit(Unit)
            }
        } else {
            db = currentDb
        }
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeMutableStateFlow.value = ExpenseEditType(isRevenue, editedExpense != null)
    }

    fun onSave(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeMutableStateFlow.value.isRevenue
        val date = expenseDateMutableStateFlow.value

        val dateOfInstallation = parameters.getInitDate() ?: LocalDate.now()

        if( date.isBefore(dateOfInstallation) ) {
            viewModelScope.launch {
                expenseAddBeforeInitDateErrorMutableFlow.emit(Unit)
            }

            return
        }

        doSaveExpense(value, description, recurringExpenseType, editedExpense, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateConfirmed(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeMutableStateFlow.value.isRevenue
        val date = expenseDateMutableStateFlow.value

        doSaveExpense(value, description, recurringExpenseType, editedExpense, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(
        value: Double,
        description: String,
        recurringExpenseType: RecurringExpenseType,
        editedExpense: Expense?,
        isRevenue: Boolean,
        date: LocalDate,
    ) {
        savingStateMutableStateFlow.value = SavingState.Saving(isRevenue)

        viewModelScope.launch {
            try {
                val inserted = withContext(Dispatchers.Default) {
                    if( editedExpense == null ) {
                        try {
                            db.persistRecurringExpense(RecurringExpense(description, if (isRevenue) -value else value, date, recurringExpenseType))
                            return@withContext true
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e

                            Logger.error("Error while inserting recurring expense into DB", e)
                            return@withContext false
                        }

                    } else {
                        try {
                            val recurringExpense = editedExpense.associatedRecurringExpense!!.recurringExpense
                            db.updateRecurringExpenseAfterDate(
                                recurringExpense.copy(
                                    modified = true,
                                    type = recurringExpenseType,
                                    recurringDate = date,
                                    title = description,
                                    amount = if (isRevenue) -value else value
                                ),
                                editedExpense.date,
                            )

                            return@withContext true
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e

                            Logger.error("Error while editing recurring expense into DB", e)
                            return@withContext false
                        }
                    }
                }

                if( inserted ) {
                    finishMutableFlow.emit(Unit)
                } else {
                    errorMutableFlow.emit(Unit)
                }
            } finally {
                savingStateMutableStateFlow.value = SavingState.Idle
            }
        }
    }

    fun onDateChanged(date: LocalDate) {
        expenseDateMutableStateFlow.value = date
    }

    sealed class SavingState {
        data object Idle : SavingState()
        data class Saving(val isRevenue: Boolean): SavingState()
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double, val type: RecurringExpenseType)