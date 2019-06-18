package com.benoitletondor.easybudgetapp.injection

import androidx.collection.ArrayMap
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.IabImpl
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.db.DB
import com.benoitletondor.easybudgetapp.model.db.impl.CachedDBImpl
import com.benoitletondor.easybudgetapp.model.db.impl.CacheDBStorage
import com.benoitletondor.easybudgetapp.model.db.impl.DBImpl
import com.benoitletondor.easybudgetapp.model.db.impl.RoomDB
import org.koin.dsl.module
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

val appModule = module {
    single { Parameters(get()) }

    single<Iab> { IabImpl(get(), get()) }

    single<CacheDBStorage> { object : CacheDBStorage {
        override val expenses: MutableMap<Date, List<Expense>> = ArrayMap()
        override val balances: MutableMap<Date, Double> = ArrayMap()
    } }

    single<Executor> { Executors.newSingleThreadExecutor() }

    factory<DB> { CachedDBImpl(DBImpl(RoomDB.create(get())), get(), get()) }
}