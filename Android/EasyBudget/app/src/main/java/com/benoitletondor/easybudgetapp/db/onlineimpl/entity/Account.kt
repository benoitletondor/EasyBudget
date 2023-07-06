package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Account() : RealmObject {
    @PrimaryKey
    var id: String = ""
    var secret: String = ""

    constructor(
        id: String,
        secret: String,
    ) : this() {
        this.id = id
        this.secret = secret
    }
}