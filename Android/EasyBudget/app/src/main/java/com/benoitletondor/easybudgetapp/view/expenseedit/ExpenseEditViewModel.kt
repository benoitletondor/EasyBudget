package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ExpenseEditViewModel(private val db: DB) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var expense: Expense? = null
    val dateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<Pair<Boolean, Boolean>>()
    val existingExpenseStream = SingleLiveEvent<Pair<String, Double>?>()
    val finishStream = MutableLiveData<Unit>()

    fun initWithDateAndExpense(date: Date, expense: Expense?) {
        this.expense = expense
        this.dateLiveData.value = expense?.date ?: date
        this.editTypeLiveData.value = Pair(expense?.isRevenue ?: false, expense != null)

        existingExpenseStream.value = if( expense != null ) Pair(expense.title, expense.amount) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = Pair(isRevenue, expense != null)
    }

    fun onSave(value: Double, description: String) {
        val isRevenue = editTypeLiveData.value?.first ?: return
        val date = dateLiveData.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val editedExpense = expense

                val expense = if( editedExpense == null ) {
                    Expense(description, if (isRevenue) -value else value, date)
                } else {
                    editedExpense.title = description
                    editedExpense.amount = if (isRevenue) -value else value
                    editedExpense.date = date

                    editedExpense
                }

                db.persistExpense(expense)
            }

            finishStream.value = null
        }
    }

    fun onDateChanged(date: Date) {
        this.dateLiveData.value = date
    }
}