/*
 *   Copyright 2024 Benoit LETONDOR
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
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor() : ViewModel() {
    private val stateMutableFlow = MutableStateFlow<MonthlyReportState>(MonthlyReportState.Loading)
    val stateFlow: Flow<MonthlyReportState> = stateMutableFlow

    private val unableToLoadDBEventMutableFlow = MutableLiveFlow<Unit>()
    val unableToLoadDBEventFlow: Flow<Unit> = unableToLoadDBEventMutableFlow

    private lateinit var db: DB

    init {
        val currentDb = AccountViewModel.getCurrentDB()
        if (currentDb == null) {
            viewModelScope.launch {
                unableToLoadDBEventMutableFlow.emit(Unit)
            }
        } else {
            db = currentDb
        }
    }

    fun loadDataForMonth(month: YearMonth) {
        if (!::db.isInitialized) {
            return
        }

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

    sealed class MonthlyReportState {
        data object Loading : MonthlyReportState()
        data object Empty: MonthlyReportState()
        class Loaded(val expenses: List<Expense>, val revenues: List<Expense>, val expensesAmount: Double, val revenuesAmount: Double) : MonthlyReportState()
    }
}