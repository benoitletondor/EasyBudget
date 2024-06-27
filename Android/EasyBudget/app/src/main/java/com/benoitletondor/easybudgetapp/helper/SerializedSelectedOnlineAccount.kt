package com.benoitletondor.easybudgetapp.helper

import android.os.Parcelable
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.net.URLEncoder

@Serializable
@Parcelize
data class SerializedSelectedOnlineAccount(
    val name: String,
    val isOwner: Boolean,
    val ownerEmail: String,
    val accountId: String,
    val accountSecret: String,
) : Parcelable {
    constructor(selectedAccount: MainViewModel.SelectedAccount.Selected.Online) : this(
        URLEncoder.encode(selectedAccount.name, "UTF-8"),
        selectedAccount.isOwner,
        selectedAccount.ownerEmail,
        selectedAccount.accountId,
        selectedAccount.accountSecret,
    )

    fun toSelectedAccount() = MainViewModel.SelectedAccount.Selected.Online(
        URLDecoder.decode(name, "UTF-8"),
        isOwner,
        ownerEmail,
        accountId,
        accountSecret,
    )
}