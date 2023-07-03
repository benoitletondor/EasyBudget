package com.benoitletondor.easybudgetapp.accounts.model

data class Account(
    val id: String,
    val name: String,
    val ownerEmail: String,
    val isUserOwner: Boolean,
)