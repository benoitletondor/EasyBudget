package com.benoitletondor.easybudgetapp.db.onlineimpl

data class Account(
    val id: String,
    val secret: String,
) {
    fun generateQuery() = "accountId == '$id' AND accountSecret == '$secret'"
}