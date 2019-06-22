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

package com.benoitletondor.easybudgetapp.view.report

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MonthlyReportViewModel(private val db: DB) : ViewModel() {
    val monthlyReportDataLiveData = MutableLiveData<MonthlyReportData>()

    sealed class MonthlyReportData {
        object Empty: MonthlyReportData()
        class Data(val expenses: List<Expense>, val revenues: List<Expense>, val expensesAmount: Double, val revenuesAmount: Double) : MonthlyReportData()
    }

    fun loadDataForMonth(month: Date) {
        viewModelScope.launch {
            val expensesForMonth = withContext(Dispatchers.Default) {
                db.getExpensesForMonth(month)
            }
            if( expensesForMonth.isEmpty() ) {
                monthlyReportDataLiveData.value = MonthlyReportData.Empty
                return@launch
            }

            val expenses = mutableListOf<Expense>()
            val revenues = mutableListOf<Expense>()
            var revenuesAmount = 0.0
            var expensesAmount = 0.0

            withContext(Dispatchers.Default) {
                for(expense in expensesForMonth) {
                    if( expense.isRevenue() ) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                    }
                }
            }

            monthlyReportDataLiveData.value = MonthlyReportData.Data(expenses, revenues, expensesAmount, revenuesAmount)
        }
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}