package com.benoitletondor.easybudgetapp.view.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

class MainViewModel(private val db: DB) : ViewModel() {
    val expenseDeletionSuccessStream : MutableLiveData<Expense> = MutableLiveData()
    val expenseDeletionErrorStream: MutableLiveData<Expense> = MutableLiveData()
    val expenseRecoverySuccessStream : MutableLiveData<Expense> = MutableLiveData()
    val expenseRecoveryErrorStream: MutableLiveData<Expense> = MutableLiveData()
    val recurringExpenseDeletionDeleteProgressStream: MutableLiveData<RecurringExpenseDeleteProgressState> = MutableLiveData()
    val recurringExpenseRestoreProgressStream: MutableLiveData<RecurringExpenseRestoreProgressState> = MutableLiveData()
    val currentBalanceEditorStream: MutableLiveData<Double> = MutableLiveData()
    val currentBalanceEditingErrorStream: MutableLiveData<Exception> = MutableLiveData()
    val currentBalanceEditedStream: MutableLiveData<Triple<Expense, Double, Double>> = MutableLiveData()
    val currentBalanceRestoringStream: MutableLiveData<Unit> = MutableLiveData()
    val currentBalanceRestoringErrorStream: MutableLiveData<Exception> = MutableLiveData()

    sealed class RecurringExpenseDeleteProgressState {
        class Starting(val expense: Expense): RecurringExpenseDeleteProgressState()

        class ErrorRecurringExpenseDeleteNotAssociated(val expense: Expense): RecurringExpenseDeleteProgressState()
        class ErrorCantDeleteBeforeFirstOccurrence(val expense: Expense): RecurringExpenseDeleteProgressState()
        class ErrorIO(val expense: Expense): RecurringExpenseDeleteProgressState()

        class Deleted(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseDeleteProgressState()
    }

    sealed class RecurringExpenseRestoreProgressState {
        class Starting(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
        class ErrorIO(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
        class Restored(val recurringExpense: RecurringExpense, val expensesToRestore: List<Expense>): RecurringExpenseRestoreProgressState()
    }

    fun onDeleteExpenseClicked(expense: Expense) {
        viewModelScope.launch(Dispatchers.Default) {
            val expenseDeleted = db.deleteExpense(expense)

            if( expenseDeleted ) {
                expenseDeletionSuccessStream.postValue(expense)
            } else {
                expenseDeletionErrorStream.postValue(expense)
            }
        }
    }

    fun onExpenseDeletionCancelled(expense: Expense) {
        viewModelScope.launch(Dispatchers.Default) {
            val expensePersisted = db.persistExpense(expense, true)

            if( expensePersisted ) {
                expenseRecoverySuccessStream.postValue(expense)
            } else {
                expenseRecoveryErrorStream.postValue(expense)
            }
        }
    }

    fun onDeleteRecurringExpenseClicked(expense: Expense, deleteType: RecurringExpenseDeleteType) {
        viewModelScope.launch(Dispatchers.Default) {
            recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.Starting(expense))

            val associatedRecurringExpense = expense.associatedRecurringExpense
            if( associatedRecurringExpense == null ) {
                recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorRecurringExpenseDeleteNotAssociated(expense))
                return@launch
            }

            if (deleteType == RecurringExpenseDeleteType.TO && !db.hasExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)) {
                recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorCantDeleteBeforeFirstOccurrence(expense))
                return@launch
            }

            val expensesToRestore: List<Expense> = when (deleteType) {
                RecurringExpenseDeleteType.ALL -> {
                    val expensesToRestore = db.getAllExpenseForRecurringExpense(associatedRecurringExpense)

                    val expensesDeleted = db.deleteAllExpenseForRecurringExpense(associatedRecurringExpense)
                    if (!expensesDeleted) {
                        recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorIO(expense))
                        return@launch
                    }

                    val recurringExpenseDeleted = db.deleteRecurringExpense(associatedRecurringExpense)
                    if (!recurringExpenseDeleted) {
                        recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorIO(expense))
                        return@launch
                    }

                    expensesToRestore
                }
                RecurringExpenseDeleteType.FROM -> {
                    val expensesToRestore = db.getAllExpensesForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)

                    val expensesDeleted = db.deleteAllExpenseForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)
                    if (!expensesDeleted) {
                        recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorIO(expense))
                        return@launch
                    }

                    expensesToRestore
                }
                RecurringExpenseDeleteType.TO -> {
                    val expensesToRestore = db.getAllExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)

                    val expensesDeleted = db.deleteAllExpenseForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)
                    if (!expensesDeleted) {
                        recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorIO(expense))
                        return@launch
                    }

                    expensesToRestore
                }
                RecurringExpenseDeleteType.ONE -> {
                    val expensesToRestore = listOf(expense)

                    val expenseDeleted = db.deleteExpense(expense)
                    if (!expenseDeleted) {
                        recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorIO(expense))
                        return@launch
                    }

                    expensesToRestore
                }
            }

            recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.Deleted(associatedRecurringExpense, expensesToRestore))
        }

    }

    fun onRestoreRecurringExpenseClicked(recurringExpense: RecurringExpense, expensesToRestore: List<Expense>) {
        viewModelScope.launch(Dispatchers.Default) {
            recurringExpenseRestoreProgressStream.postValue(RecurringExpenseRestoreProgressState.Starting(recurringExpense, expensesToRestore))

            if ( !db.addRecurringExpense(recurringExpense) ) {
                recurringExpenseRestoreProgressStream.postValue(RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore))
                return@launch
            }

            for (expense in expensesToRestore) {
                if (!db.persistExpense(expense, true)) {
                    recurringExpenseRestoreProgressStream.postValue(RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore))
                    return@launch
                }
            }

            recurringExpenseRestoreProgressStream.postValue(RecurringExpenseRestoreProgressState.Restored(recurringExpense, expensesToRestore))
        }
    }

    fun onChangeMonth(time: Date) {
        db.preloadMonth(time)
    }

    fun onAdjustCurrentBalanceClicked() {
        viewModelScope.launch(Dispatchers.Default) {
            currentBalanceEditorStream.postValue(-db.getBalanceForDay(Date()))
        }
    }

    fun onNewBalanceSelected(newBalance: Double, balanceExpenseTitle: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val currentBalance = -db.getBalanceForDay(Date())
                if (newBalance == currentBalance) {
                    // Nothing to do, balance hasn't change
                    return@launch
                }

                val diff = newBalance - currentBalance

                // Look for an existing balance for the day
                val existingExpense = db.getExpensesForDay(Date()).find { it.title == balanceExpenseTitle }

                // If the adjust balance exists, just add the diff and persist it
                if (existingExpense != null) {
                    existingExpense.amount = existingExpense.amount - diff
                    db.persistExpense(existingExpense)

                    currentBalanceEditedStream.postValue(Triple(existingExpense, diff, newBalance))
                    // If no adjust balance yet, create a new one
                } else {
                    val persistedExpense = Expense(balanceExpenseTitle, -diff, Date())
                    db.persistExpense(persistedExpense)

                    currentBalanceEditedStream.postValue(Triple(persistedExpense, diff, newBalance))
                }
            } catch (e: Exception) {
                currentBalanceEditingErrorStream.postValue(e)
            }
        }
    }

    fun onCurrentBalanceEditedCancelled(expense: Expense, diff: Double) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if( expense.amount + diff == 0.0 ) {
                    db.deleteExpense(expense)
                    return@launch
                }

                expense.amount += diff
                db.persistExpense(expense)

                currentBalanceRestoringStream.postValue(Unit)
            } catch (e: Exception) {
                currentBalanceRestoringErrorStream.postValue(e)
            }
        }
    }

// ----------------------------------------->

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}