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

package com.benoitletondor.easybudgetapp.view.recurringexpenseadd

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.db.DB
import kotlinx.coroutines.launch
import java.util.*
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecurringExpenseEditViewModel @Inject constructor(
    private val db: DB,
    private val parameters: Parameters,
) : ViewModel() {
    private var editedExpense: Expense? = null
    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<ExpenseEditType>()
    val existingExpenseEventStream = SingleLiveEvent<ExistingExpenseData?>()
    val savingIsRevenueEventStream = SingleLiveEvent<Boolean>()
    val finishLiveData = MutableLiveData<Unit>()
    val expenseAddBeforeInitDateEventStream = SingleLiveEvent<Unit>()
    val errorEventStream = SingleLiveEvent<Unit>()

    fun initWithDateAndExpense(date: Date, expense: Expense?) {
        this.expenseDateLiveData.value = date
        this.editedExpense = expense
        this.editTypeLiveData.value = ExpenseEditType(
            editedExpense?.isRevenue() ?: false,
            editedExpense != null
        )

        existingExpenseEventStream.value = if( expense != null ) ExistingExpenseData(expense.title, expense.amount, expense.associatedRecurringExpense!!.type) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = ExpenseEditType(isRevenue, editedExpense != null)
    }

    fun onSave(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
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

        doSaveExpense(value, description, recurringExpenseType, editedExpense, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateConfirmed(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        doSaveExpense(value, description, recurringExpenseType, editedExpense, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(value: Double, description: String, recurringExpenseType: RecurringExpenseType, editedExpense: Expense?, isRevenue: Boolean, date: Date) {
        savingIsRevenueEventStream.value = isRevenue

        viewModelScope.launch {
            val inserted = withContext(Dispatchers.Default) {
                if( editedExpense == null ) {
                    val insertedExpense = try {
                        db.persistRecurringExpense(RecurringExpense(description, if (isRevenue) -value else value, date, recurringExpenseType))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting recurring expense into DB: addRecurringExpense returned false")
                        return@withContext false
                    }

                    if( !flattenExpensesForRecurringExpense(insertedExpense, date) ) {
                        Logger.error(false, "Error while flattening expenses for recurring expense: flattenExpensesForRecurringExpense returned false")
                        return@withContext false
                    }

                    return@withContext true
                } else {
                    val recurringExpense = try {
                        val recurringExpense = editedExpense.associatedRecurringExpense!!
                        db.deleteAllExpenseForRecurringExpenseFromDate(recurringExpense, editedExpense.date)
                        db.deleteExpense(editedExpense)

                        val newRecurringExpense = recurringExpense.copy(
                            modified = true,
                            type = recurringExpenseType,
                            recurringDate = date,
                            title = description,
                            amount = if (isRevenue) -value else value
                        )
                        db.persistRecurringExpense(newRecurringExpense)
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while editing recurring expense into DB: addRecurringExpense returned false")
                        return@withContext false
                    }

                    if( !flattenExpensesForRecurringExpense(recurringExpense, date) ) {
                        Logger.error(false, "Error while flattening expenses for recurring expense edit: flattenExpensesForRecurringExpense returned false")
                        return@withContext false
                    }

                    return@withContext true
                }
            }

            if( inserted ) {
                finishLiveData.value = null
            } else {
                errorEventStream.value = null
            }
        }
    }

    private suspend fun flattenExpensesForRecurringExpense(expense: RecurringExpense, date: Date): Boolean
    {
        val cal = Calendar.getInstance()
        cal.time = date

        when (expense.type) {
            RecurringExpenseType.DAILY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 365*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RecurringExpenseType.BI_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
            }
            RecurringExpenseType.TER_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 3)
                }
            }
            RecurringExpenseType.FOUR_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 4)
                }
            }
            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12*10) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 1)
                }
            }
            RecurringExpenseType.BI_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 6*25) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 2)
                }
            }
            RecurringExpenseType.TER_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 4*25) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 3)
                }
            }
            RecurringExpenseType.SIX_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 2*25) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 6)
                }
            }
            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.amount, cal.time, false, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.YEAR, 1)
                }
            }
        }

        return true
    }

    fun onDateChanged(date: Date) {
        this.expenseDateLiveData.value = date
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double, val type: RecurringExpenseType)