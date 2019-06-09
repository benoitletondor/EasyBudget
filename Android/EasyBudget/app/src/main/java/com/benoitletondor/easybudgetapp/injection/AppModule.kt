package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.IabImpl
import org.koin.dsl.module

val appModule = module {

    single<Iab> { IabImpl(get()) }

}