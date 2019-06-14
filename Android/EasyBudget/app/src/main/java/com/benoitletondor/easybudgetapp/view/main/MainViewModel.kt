package com.benoitletondor.easybudgetapp.view.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*

class MainViewModel(private val db: DB,
                    private val iab: Iab) : ViewModel() {
    private var selectedDate: Date = Date()

    val premiumStatusStream = MutableLiveData<Boolean>()
    val selectedDateChangeStream = MutableLiveData<Triple<Date, Double, List<Expense>>>()

    val expenseDeletionSuccessStream = SingleLiveEvent<Pair<Expense, Double>>()
    val expenseDeletionErrorStream = SingleLiveEvent<Expense>()
    val expenseRecoverySuccessStream = SingleLiveEvent<Expense>()
    val expenseRecoveryErrorStream = SingleLiveEvent<Expense>()
    val recurringExpenseDeletionDeleteProgressStream = SingleLiveEvent<RecurringExpenseDeleteProgressState>()
    val recurringExpenseRestoreProgressStream = SingleLiveEvent<RecurringExpenseRestoreProgressState>()
    val currentBalanceEditorStream = SingleLiveEvent<Double>()
    val currentBalanceEditingErrorStream = SingleLiveEvent<Exception>()
    val currentBalanceEditedStream = SingleLiveEvent<Triple<Expense, Double, Double>>()
    val currentBalanceRestoringStream = SingleLiveEvent<Unit>()
    val currentBalanceRestoringErrorStream = SingleLiveEvent<Exception>()

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
        premiumStatusStream.value = iab.isUserPremium()
        refreshDataForDate(selectedDate)
    }

    fun onDeleteExpenseClicked(expense: Expense) {
        viewModelScope.launch {
            val expenseDeleted = withContext(Dispatchers.Default) {
                db.deleteExpense(expense)
            }

            if( expenseDeleted ) {
                expenseDeletionSuccessStream.value = Pair(expense, getBalanceForDay(selectedDate))
            } else {
                expenseDeletionErrorStream.value = expense
            }
        }
    }

    fun onExpenseDeletionCancelled(expense: Expense) {
        viewModelScope.launch {
            val expensePersisted = withContext(Dispatchers.Default) {
                db.persistExpense(expense, true)
            }

            if( expensePersisted != null ) {
                expenseRecoverySuccessStream.value = expensePersisted
                refreshDataForDate(selectedDate)
            } else {
                expenseRecoveryErrorStream.value = expense
            }
        }
    }

    fun onDeleteRecurringExpenseClicked(expense: Expense, deleteType: RecurringExpenseDeleteType) {
        viewModelScope.launch {
            recurringExpenseDeletionDeleteProgressStream.value = RecurringExpenseDeleteProgressState.Starting(expense)

            val associatedRecurringExpense = expense.associatedRecurringExpense
            if( associatedRecurringExpense == null ) {
                recurringExpenseDeletionDeleteProgressStream.value = RecurringExpenseDeleteProgressState.ErrorRecurringExpenseDeleteNotAssociated(expense)
                return@launch
            }

            val firstOccurrenceError = withContext(Dispatchers.Default) {
                deleteType == RecurringExpenseDeleteType.TO && !db.hasExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)
            }

            if ( firstOccurrenceError ) {
                recurringExpenseDeletionDeleteProgressStream.postValue(RecurringExpenseDeleteProgressState.ErrorCantDeleteBeforeFirstOccurrence(expense))
                return@launch
            }

            val expensesToRestore: List<Expense>? = withContext(Dispatchers.Default) {
                when (deleteType) {
                    RecurringExpenseDeleteType.ALL -> {
                        val expensesToRestore = db.getAllExpenseForRecurringExpense(associatedRecurringExpense)

                        val expensesDeleted = db.deleteAllExpenseForRecurringExpense(associatedRecurringExpense)
                        if (!expensesDeleted) {
                            return@withContext null
                        }

                        val recurringExpenseDeleted = db.deleteRecurringExpense(associatedRecurringExpense)
                        if (!recurringExpenseDeleted) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.FROM -> {
                        val expensesToRestore = db.getAllExpensesForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)

                        val expensesDeleted = db.deleteAllExpenseForRecurringExpenseFromDate(associatedRecurringExpense, expense.date)
                        if (!expensesDeleted) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.TO -> {
                        val expensesToRestore = db.getAllExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)

                        val expensesDeleted = db.deleteAllExpenseForRecurringExpenseBeforeDate(associatedRecurringExpense, expense.date)
                        if (!expensesDeleted) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                    RecurringExpenseDeleteType.ONE -> {
                        val expensesToRestore = listOf(expense)

                        val expenseDeleted = db.deleteExpense(expense)
                        if (!expenseDeleted) {
                            return@withContext null
                        }

                        expensesToRestore
                    }
                }
            }

            if( expensesToRestore == null ) {
                recurringExpenseDeletionDeleteProgressStream.value = RecurringExpenseDeleteProgressState.ErrorIO(expense)
                return@launch
            }

            recurringExpenseDeletionDeleteProgressStream.value = RecurringExpenseDeleteProgressState.Deleted(associatedRecurringExpense, deleteType == RecurringExpenseDeleteType.ALL, expensesToRestore)
            refreshDataForDate(selectedDate)
        }

    }

    fun onRestoreRecurringExpenseClicked(recurringExpense: RecurringExpense, restoreRecurring: Boolean, expensesToRestore: List<Expense>) {
        viewModelScope.launch {
            recurringExpenseRestoreProgressStream.value = RecurringExpenseRestoreProgressState.Starting(recurringExpense, expensesToRestore)

            if( restoreRecurring ) {
                val recurringExpenseAdd = withContext(Dispatchers.Default) {
                    db.addRecurringExpense(recurringExpense)
                }

                if ( recurringExpenseAdd == null ) {
                    recurringExpenseRestoreProgressStream.postValue(RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore))
                    return@launch
                }
            }

            val expensesAdd = withContext(Dispatchers.Default) {
                for (expense in expensesToRestore) {
                    if ( db.persistExpense(expense, true) == null ) {
                        return@withContext false
                    }
                }

                return@withContext true
            }

            if( !expensesAdd ) {
                recurringExpenseRestoreProgressStream.value = RecurringExpenseRestoreProgressState.ErrorIO(recurringExpense, expensesToRestore)
                return@launch
            }

            recurringExpenseRestoreProgressStream.value = RecurringExpenseRestoreProgressState.Restored(recurringExpense, expensesToRestore)
            refreshDataForDate(selectedDate)
        }
    }

    fun onChangeMonth(time: Date) {
        db.preloadMonth(time)
    }

    fun onAdjustCurrentBalanceClicked() {
        viewModelScope.launch {
            val balance = withContext(Dispatchers.Default) {
                -db.getBalanceForDay(Date())
            }

            currentBalanceEditorStream.value = balance
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
                    val newExpense = existingExpense.copy(amount = existingExpense.amount - diff)

                    withContext(Dispatchers.Default) {
                        db.persistExpense(newExpense)
                    }

                    currentBalanceEditedStream.value = Triple(newExpense, diff, newBalance)
                    refreshDataForDate(selectedDate)
                } else { // If no adjust balance yet, create a new one
                    val persistedExpense = Expense(balanceExpenseTitle, -diff, Date())

                    withContext(Dispatchers.Default) {
                        db.persistExpense(persistedExpense)
                    }

                    currentBalanceEditedStream.value = Triple(persistedExpense, diff, newBalance)
                }
            } catch (e: Exception) {
                currentBalanceEditingErrorStream.value = e
            }
        }
    }

    fun onCurrentBalanceEditedCancelled(expense: Expense, diff: Double) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if( expense.amount + diff == 0.0 ) {
                        db.deleteExpense(expense)
                        return@withContext
                    }

                    val newExpense = expense.copy(amount = expense.amount + diff)
                    db.persistExpense(newExpense)
                }

                currentBalanceRestoringStream.value = Unit
                refreshDataForDate(selectedDate)
            } catch (e: Exception) {
                currentBalanceRestoringErrorStream.value = e
            }
        }
    }

    fun onIabStatusChanged() {
        premiumStatusStream.value = iab.isUserPremium()
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

            selectedDateChangeStream.value = Triple(date, balance, expenses)
        }
    }

    private fun getBalanceForDay(date: Date): Double {
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