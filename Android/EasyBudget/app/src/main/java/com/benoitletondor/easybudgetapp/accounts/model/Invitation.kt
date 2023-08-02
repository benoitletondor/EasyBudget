package com.benoitletondor.easybudgetapp.accounts.model

data class Invitation(
    val id: String,
    val senderEmail: String,
    val senderId: String,
    val receiverEmail: String,
    val accountId: String,
    val status: InvitationStatus,
)

enum class InvitationStatus(val dbValue: Int) {
    SENT(0),
    ACCEPTED(1),
    REJECTED(2),
}