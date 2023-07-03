package com.benoitletondor.easybudgetapp.accounts

import android.util.Log
import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

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

    private fun watchOwnAccounts(currentUser: CurrentUser): Flow<List<Account>> = db.collection("accounts")
        .whereArrayContains("members", currentUser.email)
        .whereEqualTo("ownerId", currentUser.id)
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

                return@flatMapLatest db.collection("accounts")
                    .whereIn(FieldPath.documentId(), accountIds)
                    .watchAsFlow { value ->
                        value.documents.mapNotNull {
                            it.toAccountOrThrow(currentUser)
                        }
                    }
            }

    private fun watchAcceptedInvitations(currentUser: CurrentUser): Flow<List<String>> = db.collection("invitations")
        .whereEqualTo("receiverEmail", currentUser.email)
        .whereEqualTo("status", InvitationStatus.ACCEPTED.dbValue)
        .watchAsFlow { value ->
            value.documents.mapNotNull {
                it.getString("accountId")
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
        val ownerEmail = getString("ownerEmail")!!

        return Account(
            id = id,
            name = getString("name")!!,
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