/*
 *   Copyright 2021 Benoit LETONDOR
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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    private val db: DB,
    private val parameters: Parameters,
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var expense: Expense? = null

    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<ExpenseEditType>()
    val existingExpenseEventStream = SingleLiveEvent<ExistingExpenseData?>()
    val expenseAddBeforeInitDateEventStream = SingleLiveEvent<Unit>()
    val finishEventStream = MutableLiveData<Unit>()

    fun initWithDateAndExpense(date: Date, expense: Expense?) {
        this.expense = expense
        this.expenseDateLiveData.value = expense?.date ?: date
        this.editTypeLiveData.value = ExpenseEditType(expense?.isRevenue() ?: false, expense != null)

        existingExpenseEventStream.value = if( expense != null ) ExistingExpenseData(expense.title, expense.amount) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = ExpenseEditType(isRevenue, expense != null)
    }

    fun onSave(value: Double, description: String) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        val dateOfInstallationCalendar = Calendar.getInstance()
        dateOfInstallationCalendar.time = Date(parameters.getInitTimestamp())
        dateOfInstallationCalendar.set(Calendar.HOUR_OF_DAY, 0)
        dateOfInstallationCalendar.set(Calendar.MINUTE, 0)
        dateOfInstallationCalendar.set(Calendar.SECOND, 0)
        dateOfInstallationCalendar.set(Calendar.MILLISECOND, 0)

        if( date.before(dateOfInstallationCalendar.time) ) {
            expenseAddBeforeInitDateEventStream.value = Unit
            return
        }

        doSaveExpense(value, description, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateConfirmed(value: Double, description: String) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        doSaveExpense(value, description, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(value: Double, description: String, isRevenue: Boolean, date: Date) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val expense = expense?.copy(
                    title = description,
                    amount = if (isRevenue) -value else value,
                    date = date
                ) ?: Expense(description, if (isRevenue) -value else value, date, false)

                db.persistExpense(expense)
            }

            finishEventStream.value = null
        }
    }

    fun onDateChanged(date: Date) {
        this.expenseDateLiveData.value = date
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double)