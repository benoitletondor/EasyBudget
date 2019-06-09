package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }
}