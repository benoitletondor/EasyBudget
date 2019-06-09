package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.helper.Parameters
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.IabImpl
import org.koin.dsl.module

val appModule = module {
    single { Parameters(get()) }

    single<Iab> { IabImpl(get(), get()) }
}