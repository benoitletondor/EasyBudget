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

package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditViewModel
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.premium.PremiumViewModel
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseAddViewModel
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportViewModel
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseViewModel
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { SelectCurrencyViewModel() }
    viewModel { MonthlyReportViewModel(get()) }
    viewModel { MonthlyReportBaseViewModel(get()) }
    viewModel { ExpenseEditViewModel(get()) }
    viewModel { RecurringExpenseAddViewModel(get()) }
    viewModel { PremiumViewModel(get()) }
}