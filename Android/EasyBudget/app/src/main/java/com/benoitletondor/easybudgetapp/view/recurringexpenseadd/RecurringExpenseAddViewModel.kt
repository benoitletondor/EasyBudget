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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RecurringExpenseAddViewModel(private val db: DB) : ViewModel() {
    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeIsRevenueLiveData = MutableLiveData<Boolean>()
    val savingIsRevenueEventStream = SingleLiveEvent<Boolean>()
    val finishLiveData = MutableLiveData<Unit>()
    val errorEventStream = SingleLiveEvent<Unit>()

    fun initWithDateAndExpense(date: Date) {
        this.expenseDateLiveData.value = date
        this.editTypeIsRevenueLiveData.value = false
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeIsRevenueLiveData.value = isRevenue
    }

    fun onSave(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeIsRevenueLiveData.value ?: return
        val date = expenseDateLiveData.value ?: return

        savingIsRevenueEventStream.value = isRevenue

        viewModelScope.launch {
            val inserted = withContext(Dispatchers.Default) {
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
            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
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
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
            }
            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12*10) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 1)
                }
            }
            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
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