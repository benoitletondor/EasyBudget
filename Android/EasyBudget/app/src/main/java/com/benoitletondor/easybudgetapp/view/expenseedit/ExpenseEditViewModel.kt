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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    private val parameters: Parameters,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private val editedExpense: Expense? = savedStateHandle.get<Expense>(ExpenseEditActivity.ARG_EDITED_EXPENSE)

    private val expenseDateMutableStateFlow = MutableStateFlow(LocalDate.ofEpochDay(
        savedStateHandle[ExpenseEditActivity.ARG_DATE] ?: throw IllegalStateException("No ARG_DATE arg")))
    val expenseDateFlow: Flow<LocalDate> = expenseDateMutableStateFlow

    private val editTypeMutableStateFlow = MutableStateFlow(ExpenseEditType(
        editedExpense?.isRevenue() ?: false,
        editedExpense != null
    ))
    val editTypeFlow: Flow<ExpenseEditType> = editTypeMutableStateFlow

    val existingExpenseData = editedExpense?.let { expense ->
        ExistingExpenseData(
            expense.title,
            expense.amount,
        )
    }

    private val expenseAddBeforeInitDateErrorMutableFlow = MutableLiveFlow<Unit>()
    val expenseAddBeforeInitDateEventFlow: Flow<Unit> = expenseAddBeforeInitDateErrorMutableFlow

    private val unableToLoadDBEventMutableFlow = MutableLiveFlow<Unit>()
    val unableToLoadDBEventFlow: Flow<Unit> = unableToLoadDBEventMutableFlow

    private val finishMutableFlow = MutableLiveFlow<Unit>()
    val finishFlow: Flow<Unit> = finishMutableFlow

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

    fun onSave(value: Double, description: String) {
        val isRevenue = editTypeMutableStateFlow.value.isRevenue
        val date = expenseDateMutableStateFlow.value

        val dateOfInstallation = parameters.getInitDate() ?: LocalDate.now()

        if( date.isBefore(dateOfInstallation) ) {
            viewModelScope.launch {
                expenseAddBeforeInitDateErrorMutableFlow.emit(Unit)
            }

            return
        }

        doSaveExpense(value, description, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateConfirmed(value: Double, description: String) {
        val isRevenue = editTypeMutableStateFlow.value.isRevenue
        val date = expenseDateMutableStateFlow.value

        doSaveExpense(value, description, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(value: Double, description: String, isRevenue: Boolean, date: LocalDate) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val expense = editedExpense?.copy(
                    title = description,
                    amount = if (isRevenue) -value else value,
                    date = date,
                ) ?: Expense(description, if (isRevenue) -value else value, date, false)

                db.persistExpense(expense)

                withContext(Dispatchers.Main) {
                    finishMutableFlow.emit(Unit)
                }
            }
        }
    }

    fun onDateChanged(date: LocalDate) {
        expenseDateMutableStateFlow.value = date
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double)