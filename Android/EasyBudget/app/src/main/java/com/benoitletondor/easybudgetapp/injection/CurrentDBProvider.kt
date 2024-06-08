package com.benoitletondor.easybudgetapp.injection

import com.benoitletondor.easybudgetapp.db.DB

data class CurrentDBProvider(var activeDB: DB?)
val CurrentDBProvider.requireDB: DB get() = activeDB ?: throw IllegalStateException("No DB provided")