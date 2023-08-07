package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.db.DB

interface OnlineDB : DB {
    val account: Account

    suspend fun deleteAllEntries()
}