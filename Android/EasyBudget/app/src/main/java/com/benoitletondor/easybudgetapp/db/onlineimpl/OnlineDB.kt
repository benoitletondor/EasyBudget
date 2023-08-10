package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.db.DB
import java.io.Closeable

interface OnlineDB : DB, Closeable {
    val account: Account

    suspend fun deleteAllEntries()
}