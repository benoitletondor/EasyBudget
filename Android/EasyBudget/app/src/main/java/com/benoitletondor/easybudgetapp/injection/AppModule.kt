package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.IabImpl
import com.benoitletondor.easybudgetapp.model.db.DB
import com.benoitletondor.easybudgetapp.model.db.DBCache
import org.koin.dsl.module

val appModule = module {
    single { Parameters(get()) }

    single<Iab> { IabImpl(get(), get()) }

    single { DBCache() }

    factory { DB(get(), get()) }
}