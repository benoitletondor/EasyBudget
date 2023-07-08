package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Account() : EmbeddedRealmObject {
    var id: String = ""
    var secret: String = ""

    constructor(
        id: String,
        secret: String,
    ) : this() {
        this.id = id
        this.secret = secret
    }

    fun generateQuery(): String = "account.id = '$id' AND account.secret = '$secret'"
}