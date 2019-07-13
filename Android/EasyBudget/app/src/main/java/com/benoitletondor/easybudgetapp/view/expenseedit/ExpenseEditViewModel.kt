/*
 *   Copyright 2019 Benoit LETONDOR
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ExpenseEditViewModel(private val db: DB) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var expense: Expense? = null

    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<ExpenseEditType>()
    val existingExpenseEventStream = SingleLiveEvent<ExistingExpenseData?>()
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

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val expense = expense?.copy(
                    title = description,
                    amount = if (isRevenue) -value else value,
                    date = date
                ) ?: Expense(description, if (isRevenue) -value else value, date)

                db.persistExpense(expense)
            }

            finishEventStream.value = null
        }
    }

    fun onDateChanged(date: Date) {
        this.expenseDateLiveData.value = date
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double)