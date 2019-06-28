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

package com.benoitletondor.easybudgetapp.view.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*

class MainViewModel(private val db: DB,
                    private val iab: Iab) : ViewModel() {
    private var selectedDate: Date = Date()

    val premiumStatusLiveData = MutableLiveData<Boolean>()
    val selectedDateChangeLiveData = MutableLiveData<SelectedDateExpensesData>()

    val expenseDeletionSuccessEventStream = SingleLiveEvent<ExpenseDeletionSuccessData>()
    val expenseDeletionErrorEventStream = SingleLiveEvent<Expense>()
    val expenseRecoverySuccessEventStream = SingleLiveEvent<Expense>()
    val expenseRecoveryErrorEventStream = SingleLiveEvent<Expense>()
    val recurringExpenseDeletionProgressEventStream = SingleLiveEvent<RecurringExpenseDeleteProgressState>()
    val recurringExpenseRestoreProgressEventStream = SingleLiveEvent<RecurringExpenseRestoreProgressState>()
    val startCurrentBalanceEditorEventStream = SingleLiveEvent<Double>()
    val currentBalanceEditingErrorEventStream = SingleLiveEvent<Exception>()
    val currentBalanceEditedEventStream = SingleLiveEvent<BalanceAdjustedData>()
    val currentBalanceRestoringEventStream = SingleLiveEvent<Unit>()
    val currentBalanceRestoringErrorEventStream = SingleLiveEvent<Exception>()

    sealed class RecurringExpenseDeleteProgressState {
        class Starting(val expense: Expense): RecurringExpenseDeleteProgressState()

        class ErrorRecurringExpenseDeleteNotAssociated(val expense: Expense): RecurringExpenseDeleteProgressState()
        class ErrorCantDeleteBeforeFirstOccurrence(val expense: Expense): RecurringExpenseDeleteProgressState()
        class ErrorIO(val expense: Expense): RecurringExpenseDeleteProgressState()

        class Deleted(val recurringExpense: RecurringExpense, val restoreRecurring: Boolean, val expensesToRestore: List<Expense>): RecurringExpenseDeleteProgressState()
    }

    sealed class RecurringExpenseRestoreProgressState {
        class Starting(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
        class ErrorIO(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
        class Restored(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
    }

    init {
        premiumStatusLiveData.value = iab.isUserPremium()
        refreshDataForDate(selectedDate)
    }

    fun onDeleteExpenseClicked(expense: Expense) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    db.deleteExpense(expense)
                }

                expenseDeletionSuccessEventStream.value = ExpenseDeletionSuccessData(expense, getBalanceForDay(selectedDate))
            } catch (t: Throwable) {
                expenseDeletionErrorEventStream.value = expense
            }
        }
    }

    fun onExpenseDeletionCancelled(expense: Expense) {
        viewModelScope.launch {
            try {
                val expensePersisted = withContext(Dispatchers.Default) {
                    db.persistExpense(expense)
                }

                expenseRecoverySuccessEventStream.value = expensePersisted
                refreshDataForDate(selectedDate)
            } catch (t: Throwable) {
                expenseRecoveryErrorEventStream.value = expense
            }
        }
    }

    fun onDeleteRecurringExpenseClicked(expense: Expense, deleteType: RecurringExpenseDeleteType) {
        viewModelScope.launch {
            recurringExpenseDeletionProgressEventStream.value = RecurringExpenseDeleteProgressState.Starting(expense)

            val associatedRecurringExpense = expense.associatedRecurringExpense
            if( associatedRecurringExpense == null ) {
                recurringExpenseDeletionProgressEventStream.value = RecurringExpenseDeleteProgressState.ErrorRecurringExpenseDeleteNotAssociated(expense)
                return@launch
            }

            val firstOccurrenceError = withContext(Dispatchers.Default) {
                deleteType == RecurringExpenseDeleteType.TO && !db.hasExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)
            }

            if ( firstOccurrenceError ) {
                recurringExpenseDeletionProgressEventStream.postValue(RecurringExpenseDeleteProgressState.ErrorCantDeleteBeforeFirstOccurrence(expense))
                return@launch
            }

            val expensesToRestore: List<Expense>? = withContext(Dispatchers.Default) {
                when (deleteType) {
                    RecurringExpenseDeleteType.ALL -> {
                        val expensesToRestore = db.getAllExpenseForRecurringExpense(associatedRecurringExpense)

                        try {
                            db.deleteAllExpenseForRecurringExpense(associatedRecurringExpense)
                        } catch (t: Throwable) {
                            return@withContext null
                        }

                        try {
                            db.deleteRecurringExpense(associatedRecurringExpense)
                        } catch (t: Throwable) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.FROM -> {
                        val expensesToRestore = db.getAllExpensesForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)

                        try {
                            db.deleteAllExpenseForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)
                        } catch (t: Throwable) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.TO -> {
                        val expensesToRestore = db.getAllExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)

                        try {
                            db.deleteAllExpenseForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)
                        } catch (t: Throwable) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.ONE -> {
                        val expensesToRestore = listOf(expense)

                        try {
                            db.deleteExpense(expense)
                        } catch (t: Throwable) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                }
            }

            if( expensesToRestore == null ) {
                recurringExpenseDeletionProgressEventStream.value = RecurringExpenseDeleteProgressState.ErrorIO(expense)
                return@launch
            }

            recurringExpenseDeletionProgressEventStream.value = RecurringExpenseDeleteProgressState.Deleted(associatedRecurringExpense, deleteType == RecurringExpenseDeleteType.ALL, expensesToRestore)
            refreshDataForDate(selectedDate)
        }

    }

    fun onRestoreRecurringExpenseClicked(recurringExpense: RecurringExpense, restoreRecurring: Boolean, expensesToRestore: List<Expense>) {
        viewModelScope.launch {
            recurringExpenseRestoreProgressEventStream.value = RecurringExpenseRestoreProgressState.Starting(recurringExpense, expensesToRestore)

            if( restoreRecurring ) {
                try {
                    withContext(Dispatchers.Default) {
                        db.persistRecurringExpense(recurringExpense)
                    }
                } catch (t: Throwable) {
                    recurringExpenseRestoreProgressEventStream.postValue(RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore))
                    return@launch
                }
            }

            val expensesAdd = withContext(Dispatchers.Default) {
                for (expense in expensesToRestore) {
                    try {
                        db.persistExpense(expense)
                    } catch (t: Throwable) {
                        return@withContext false
                    }
                }

                return@withContext true
            }

            if( !expensesAdd ) {
                recurringExpenseRestoreProgressEventStream.value = RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore)
                return@launch
            }

            recurringExpenseRestoreProgressEventStream.value = RecurringExpenseRestoreProgressState.Restored(recurringExpense, expensesToRestore)
            refreshDataForDate(selectedDate)
        }
    }

    fun onAdjustCurrentBalanceClicked() {
        viewModelScope.launch {
            val balance = withContext(Dispatchers.Default) {
                -db.getBalanceForDay(Date())
            }

            startCurrentBalanceEditorEventStream.value = balance
        }
    }

    fun onNewBalanceSelected(newBalance: Double, balanceExpenseTitle: String) {
        viewModelScope.launch {
            try {
                val currentBalance = withContext(Dispatchers.Default) {
                    -db.getBalanceForDay(Date())
                }

                if (newBalance == currentBalance) {
                    // Nothing to do, balance hasn't change
                    return@launch
                }

                val diff = newBalance - currentBalance

                // Look for an existing balance for the day
                val existingExpense = withContext(Dispatchers.Default) {
                    db.getExpensesForDay(Date()).find { it.title == balanceExpenseTitle }
                }

                if (existingExpense != null) { // If the adjust balance exists, just add the diff and persist it
                    val newExpense = withContext(Dispatchers.Default) {
                        db.persistExpense(existingExpense.copy(amount = existingExpense.amount - diff))
                    }

                    currentBalanceEditedEventStream.value = BalanceAdjustedData(newExpense, diff, newBalance)
                } else { // If no adjust balance yet, create a new one
                    val persistedExpense = withContext(Dispatchers.Default) {
                        db.persistExpense(Expense(balanceExpenseTitle, -diff, Date()))
                    }

                    currentBalanceEditedEventStream.value = BalanceAdjustedData(persistedExpense, diff, newBalance)
                }

                refreshDataForDate(selectedDate)
            } catch (e: Exception) {
                currentBalanceEditingErrorEventStream.value = e
            }
        }
    }

    fun onCurrentBalanceEditedCancelled(expense: Expense, diff: Double) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if( expense.amount + diff == 0.0 ) {
                        db.deleteExpense(expense)
                    } else {
                        val newExpense = expense.copy(amount = expense.amount + diff)
                        db.persistExpense(newExpense)
                    }
                }

                currentBalanceRestoringEventStream.value = Unit
                refreshDataForDate(selectedDate)
            } catch (e: Exception) {
                currentBalanceRestoringErrorEventStream.value = e
            }
        }
    }

    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    fun onSelectDate(date: Date) {
        selectedDate = date
        refreshDataForDate(date)
    }

    fun onCurrencySelected() {
        refreshDataForDate(selectedDate)
    }

    private fun refreshDataForDate(date: Date) {
        viewModelScope.launch {
            val (balance, expenses) = withContext(Dispatchers.Default) {
                Pair(getBalanceForDay(date), db.getExpensesForDay(date))
            }

            selectedDateChangeLiveData.value = SelectedDateExpensesData(date, balance, expenses)
        }
    }

    private suspend fun getBalanceForDay(date: Date): Double {
        var balance = 0.0 // Just to keep a positive number if balance == 0
        balance -= db.getBalanceForDay(date)

        return balance
    }

    fun onDayChanged() {
        selectedDate = Date()
        refreshDataForDate(selectedDate)
    }

    fun onExpenseAdded() {
        refreshDataForDate(selectedDate)
    }

    fun onWelcomeScreenFinished() {
        refreshDataForDate(selectedDate)
    }

// ----------------------------------------->

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}

data class SelectedDateExpensesData(val date: Date, val balance: Double, val expenses: List<Expense>)
data class ExpenseDeletionSuccessData(val deletedExpense: Expense, val newDayBalance: Double)
data class BalanceAdjustedData(val balanceExpense: Expense, val diffWithOldBalance: Double, val newBalance: Double)