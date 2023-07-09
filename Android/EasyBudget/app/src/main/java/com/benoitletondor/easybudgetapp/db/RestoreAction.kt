package com.benoitletondor.easybudgetapp.db

interface RestoreAction {
    suspend fun restore()
}

fun restoreAction(restoreLambda: suspend () -> Unit) : RestoreAction = object : RestoreAction {
    override suspend fun restore() {
        restoreLambda()
    }
}