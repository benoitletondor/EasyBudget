package com.benoitletondor.easybudgetapp.view.report

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MonthlyReportViewModel(private val db: DB) : ViewModel() {
    val monthlyReportLiveData = MutableLiveData<MonthlyReportData>()

    sealed class MonthlyReportData {
        object Empty: MonthlyReportData()
        class Data(val expenses: List<Expense>, val revenues: List<Expense>, val expensesAmount: Double, val revenuesAmount: Double) : MonthlyReportData()
    }

    fun loadDataForMonth(month: Date) {
        viewModelScope.launch(Dispatchers.Default) {
            val expensesForMonth = db.getExpensesForMonth(month)
            if( expensesForMonth.isEmpty() ) {
                monthlyReportLiveData.postValue(MonthlyReportData.Empty)
                return@launch
            }

            val expenses = mutableListOf<Expense>()
            val revenues = mutableListOf<Expense>()
            var revenuesAmount = 0.0
            var expensesAmount = 0.0

            for(expense in expensesForMonth) {
                if( expense.isRevenue )
                {
                    revenues.add(expense)
                    revenuesAmount -= expense.amount
                }
                else
                {
                    expenses.add(expense);
                    expensesAmount += expense.amount
                }
            }

            monthlyReportLiveData.postValue(MonthlyReportData.Data(expenses, revenues, expensesAmount, revenuesAmount))
        }
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}