/*
 *   Copyright 2022 Benoit LETONDOR
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.db.DB
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(private val db: DB) : ViewModel() {
    private val stateMutableFlow = MutableStateFlow<MonthlyReportState>(MonthlyReportState.Loading)
    val stateFlow: Flow<MonthlyReportState> = stateMutableFlow

    sealed class MonthlyReportState {
        object Loading : MonthlyReportState()
        object Empty: MonthlyReportState()
        class Loaded(val expenses: List<Expense>, val revenues: List<Expense>, val expensesAmount: Double, val revenuesAmount: Double) : MonthlyReportState()
    }

    fun loadDataForMonth(month: LocalDate) {
        viewModelScope.launch {
            val expensesForMonth = withContext(Dispatchers.Default) {
                db.getExpensesForMonth(month)
            }
            if( expensesForMonth.isEmpty() ) {
                stateMutableFlow.emit(MonthlyReportState.Empty)
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

            stateMutableFlow.emit(MonthlyReportState.Loaded(expenses, revenues, expensesAmount, revenuesAmount))
        }
    }
}