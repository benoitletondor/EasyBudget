package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

private const val INVITATIONS_COLLECTION = "invitations"
private const val INVITATION_DOCUMENT_RECEIVER_EMAIL = "receiverEmail"
private const val INVITATION_DOCUMENT_STATUS = "status"
private const val INVITATION_DOCUMENT_ACCOUNT_ID = "accountId"

private const val ACCOUNTS_COLLECTION = "accounts"
private const val ACCOUNT_DOCUMENT_SECRET = "secret"
private const val ACCOUNT_DOCUMENT_OWNER_ID = "owner_id"
private const val ACCOUNT_DOCUMENT_OWNER_EMAIL = "ownerEmail"
private const val ACCOUNT_DOCUMENT_NAME = "name"

class FirebaseAccounts(
    private val db: FirebaseFirestore,
) : Accounts {

    init {
        db.useEmulator("10.0.2.2", 8080)
    }

    override fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>> {
        val invitedAccountsFlow = watchInvitedAccounts(currentUser)
        val ownAccountsFlow = watchOwnAccounts(currentUser)

        return ownAccountsFlow
            .combine(invitedAccountsFlow) { a, b ->
                (a + b).distinctBy { it.id }
            }
    }

    private fun watchOwnAccounts(currentUser: CurrentUser): Flow<List<Account>> = db.collection(ACCOUNTS_COLLECTION)
        .whereEqualTo(ACCOUNT_DOCUMENT_OWNER_ID, currentUser.id)
        .watchAsFlow { value ->
            value.documents.mapNotNull {
                it.toAccountOrThrow(currentUser)
            }
        }

    private fun watchInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>> =
        watchAcceptedInvitations(currentUser)
            .flatMapLatest { accountIds ->
                if (accountIds.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList())
                }

                return@flatMapLatest db.collection(ACCOUNTS_COLLECTION)
                    .whereIn(FieldPath.documentId(), accountIds)
                    .watchAsFlow { value ->
                        value.documents.mapNotNull {
                            it.toAccountOrThrow(currentUser)
                        }
                    }
            }

    private fun watchAcceptedInvitations(currentUser: CurrentUser): Flow<List<String>> = db.collection(INVITATIONS_COLLECTION)
        .whereEqualTo(INVITATION_DOCUMENT_RECEIVER_EMAIL, currentUser.email)
        .whereEqualTo(INVITATION_DOCUMENT_STATUS, InvitationStatus.ACCEPTED.dbValue)
        .watchAsFlow { value ->
            value.documents.mapNotNull {
                it.getString(INVITATION_DOCUMENT_ACCOUNT_ID)
            }
        }

    private fun <T> Query.watchAsFlow(processData: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val snapshotListener = addSnapshotListener { value, error ->
            if (error != null) {
                cancel("An error was returned by FB", error)
                return@addSnapshotListener
            }

            if (value == null) {
                cancel("Missing value for snapshot listener")
                return@addSnapshotListener
            }

            val data = try {
                processData(value)
            } catch (e: Exception) {
                cancel("Error while processing data", e)
                return@addSnapshotListener
            }

            trySendBlocking(data)
        }

        awaitClose {
            snapshotListener.remove()
        }
    }

    private fun DocumentSnapshot.toAccountOrThrow(currentUser: CurrentUser): Account {
        val ownerEmail = getString(ACCOUNT_DOCUMENT_OWNER_EMAIL)!!

        return Account(
            id = id,
            secret = getString(ACCOUNT_DOCUMENT_SECRET)!!,
            name = getString(ACCOUNT_DOCUMENT_NAME)!!,
            ownerEmail = ownerEmail,
            isUserOwner = ownerEmail == currentUser.email,
        )
    }

    private enum class InvitationStatus(val dbValue: Int) {
        SENT(0),
        ACCEPTED(1),
        REJECTED(2),
    }
}