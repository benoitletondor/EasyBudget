package com.benoitletondor.easybudgetapp.view.recurringexpenseedit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.launch
import java.util.*
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RecurringExpenseEditViewModel(private val db: DB) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var expense: Expense? = null
    val dateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<Pair<Boolean, Boolean>>()
    val existingExpenseStream = SingleLiveEvent<Triple<String, Double, RecurringExpenseType>?>()
    val savingStream = SingleLiveEvent<Boolean>()
    val finishStream = MutableLiveData<Unit>()
    val errorStream = SingleLiveEvent<Unit>()

    fun initWithDateAndExpense(date: Date, expense: Expense?) {
        this.expense = expense
        this.dateLiveData.value = expense?.date ?: date
        this.editTypeLiveData.value = Pair(expense?.isRevenue ?: false, expense != null)

        existingExpenseStream.value = if( expense != null ) Triple(expense.title, expense.amount, expense.associatedRecurringExpense!!.type) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = Pair(isRevenue, expense != null)
    }

    fun onSave(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeLiveData.value?.first ?: return
        val date = dateLiveData.value ?: return

        savingStream.value = isRevenue

        viewModelScope.launch {
            val inserted = withContext(Dispatchers.Default) {
                val expense = RecurringExpense(description, if (isRevenue) -value else value, date, recurringExpenseType)

                val inserted = db.addRecurringExpense(expense)
                if( !inserted )
                {
                    Logger.error(false, "Error while inserting recurring expense into DB: addRecurringExpense returned false");
                    return@withContext false
                }

                if( !flattenExpensesForRecurringExpense(expense, date) ) {
                    Logger.error(false, "Error while flattening expenses for recurring expense: flattenExpensesForRecurringExpense returned false");
                    return@withContext false
                }

                return@withContext true
            }

            if( inserted ) {
                finishStream.value = null
            } else {
                errorStream.value = null
            }
        }
    }

    private fun flattenExpensesForRecurringExpense(expense: RecurringExpense, date: Date): Boolean
    {
        val cal = Calendar.getInstance()
        cal.time = date

        when (expense.type)
        {
            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    val expenseInserted = db.persistExpense(Expense(expense.title, expense.amount, cal.time, expense))
                    if (!expenseInserted) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RecurringExpenseType.BI_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    val expenseInserted = db.persistExpense(Expense(expense.title, expense.amount, cal.time, expense))
                    if (!expenseInserted) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
            }
            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12*10) {
                    val expenseInserted = db.persistExpense(Expense(expense.title, expense.amount, cal.time, expense))
                    if (!expenseInserted) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.MONTH, 1)
                }
            }
            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    val expenseInserted = db.persistExpense(Expense(expense.title, expense.amount, cal.time, expense))
                    if (!expenseInserted) {
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
        this.dateLiveData.value = date
    }
}