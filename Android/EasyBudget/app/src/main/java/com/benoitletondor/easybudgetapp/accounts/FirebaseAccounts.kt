package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.BuildConfig
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
import kotlinx.coroutines.tasks.await
import java.math.BigInteger
import java.security.SecureRandom

class FirebaseAccounts(
    private val db: FirebaseFirestore,
) : Accounts {

    init {
        if (BuildConfig.DEBUG) {
            db.useEmulator("10.0.2.2", 8080)
        }
    }

    override fun watchAccounts(currentUser: CurrentUser): Flow<List<Account>> {
        val invitedAccountsFlow = watchInvitedAccounts(currentUser)
        val ownAccountsFlow = watchOwnAccounts(currentUser)

        return ownAccountsFlow
            .combine(invitedAccountsFlow) { a, b ->
                (a + b).distinctBy { it.id }
            }
    }

    override suspend fun createAccount(currentUser: CurrentUser, name: String): Account {
        val (id, secret) = generateSecureAccountIdAndSecret()

        db.collection(ACCOUNTS_COLLECTION)
            .document(id)
            .set(mapOf(
                ACCOUNT_DOCUMENT_SECRET to secret,
                ACCOUNT_DOCUMENT_NAME to name,
                ACCOUNT_DOCUMENT_OWNER_ID to currentUser.id,
                ACCOUNT_DOCUMENT_OWNER_EMAIL to currentUser.email,
            ))
            .await()

        return Account(
            id = id,
            secret = secret,
            name = name,
            ownerEmail = currentUser.email,
            isUserOwner = true,
        )
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

    private fun generateSecureAccountIdAndSecret(): Pair<String, String> {
        val accountId = generateRandomString(50)
        val accountSecret = generateRandomString(100)

        return Pair(accountId, accountSecret)
    }

    private fun generateRandomString(length: Int): String {
        val secureRandom = SecureRandom()
        return BigInteger(length * 5, secureRandom).toString(32)
            .take(length)
            .map { SECURE_STRING_ALLOWED_CHARS[BigInteger(it.toString(), 32).toInt()] }
            .joinToString("")
    }

    private enum class InvitationStatus(val dbValue: Int) {
        SENT(0),
        ACCEPTED(1),
        REJECTED(2),
    }

    companion object {
        private const val INVITATIONS_COLLECTION = "invitations"
        private const val INVITATION_DOCUMENT_RECEIVER_EMAIL = "receiverEmail"
        private const val INVITATION_DOCUMENT_STATUS = "status"
        private const val INVITATION_DOCUMENT_ACCOUNT_ID = "accountId"

        private const val ACCOUNTS_COLLECTION = "accounts"
        private const val ACCOUNT_DOCUMENT_SECRET = "secret"
        private const val ACCOUNT_DOCUMENT_OWNER_ID = "owner_id"
        private const val ACCOUNT_DOCUMENT_OWNER_EMAIL = "ownerEmail"
        private const val ACCOUNT_DOCUMENT_NAME = "name"

        private const val SECURE_STRING_ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}