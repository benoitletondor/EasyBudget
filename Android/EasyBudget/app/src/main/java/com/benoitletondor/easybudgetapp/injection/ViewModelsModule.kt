package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportViewModel
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { SelectCurrencyViewModel() }
    viewModel { MonthlyReportViewModel(get()) }
}