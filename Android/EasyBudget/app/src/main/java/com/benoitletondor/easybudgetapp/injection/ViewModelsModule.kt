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