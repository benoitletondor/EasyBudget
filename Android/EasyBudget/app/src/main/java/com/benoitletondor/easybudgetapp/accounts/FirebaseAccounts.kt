package com.benoitletondor.easybudgetapp.accounts

import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.accounts.model.Invitation
import com.benoitletondor.easybudgetapp.accounts.model.InvitationStatus
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

        return combine(ownAccountsFlow, invitedAccountsFlow) { a, b ->
            (a + b).distinctBy {
                it.id
            }
        }
    }

    override fun watchAccount(
        currentUser: CurrentUser,
        accountId: String,
        accountSecret: String,
    ): Flow<Account> {
        return db.collection(ACCOUNTS_COLLECTION)
            .whereArrayContains(ACCOUNT_DOCUMENT_MEMBERS, currentUser.email)
            .whereEqualTo(ACCOUNT_DOCUMENT_SECRET, accountSecret)
            .whereEqualTo(FieldPath.documentId(), accountId)
            .watchAsFlow { value ->
                value.documents.first().toAccountOrThrow(currentUser)
            }
    }

    override fun watchInvitationsForAccount(
        currentUser: CurrentUser,
        accountId: String,
    ): Flow<List<Invitation>> {
        return db.collection(INVITATIONS_COLLECTION)
            .whereEqualTo(INVITATION_DOCUMENT_ACCOUNT_ID, accountId)
            .whereEqualTo(INVITATION_DOCUMENT_SENDER_ID, currentUser.id)
            .watchAsFlow { value ->
                value.documents.map { it.toInvitationOrThrow() }
            }
    }

    override fun watchHasPendingInvitedAccounts(currentUser: CurrentUser): Flow<Boolean>
        = watchPendingInvitations(currentUser)
            .map {
                it.isNotEmpty()
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
                ACCOUNT_DOCUMENT_MEMBERS to listOf(currentUser.email),
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

    override suspend fun deleteInvitation(currentUser: CurrentUser, invitation: Invitation) {
        val accountRef = db.collection(ACCOUNTS_COLLECTION).document(invitation.accountId)
        val invitationRef = db.collection(INVITATIONS_COLLECTION).document(invitation.id)

        db.runTransaction { transaction ->
            transaction.update(accountRef, mapOf(
                ACCOUNT_DOCUMENT_MEMBERS to FieldValue.arrayRemove(invitation.receiverEmail),
            ))

            transaction.delete(invitationRef)
        }.await()
    }

    override suspend fun updateAccountName(
        currentUser: CurrentUser,
        accountId: String,
        newName: String,
    ) {
        db.collection(ACCOUNTS_COLLECTION).document(accountId)
            .set(mapOf(
                ACCOUNT_DOCUMENT_NAME to newName,
            ))
            .await()
    }

    override suspend fun leaveAccount(currentUser: CurrentUser, accountId: String) {
        val accountRef = db.collection(ACCOUNTS_COLLECTION).document(accountId)

        val invitationQuery = db.collection(INVITATIONS_COLLECTION)
            .whereEqualTo(INVITATION_DOCUMENT_RECEIVER_EMAIL, currentUser.email)
            .whereEqualTo(INVITATION_DOCUMENT_ACCOUNT_ID, accountId)
            .get()
            .await()

        val invitationRef = invitationQuery.documents.first().reference

        db.runTransaction { transaction ->
            transaction.update(accountRef, mapOf(
                ACCOUNT_DOCUMENT_MEMBERS to FieldValue.arrayRemove(currentUser.email),
            ))

            transaction.delete(invitationRef)
        }.await()
    }

    override suspend fun deleteAccount(currentUser: CurrentUser, accountId: String) {
        db.collection(ACCOUNTS_COLLECTION)
            .document(accountId)
            .delete()
            .await()
    }

    private fun watchOwnAccounts(currentUser: CurrentUser): Flow<List<Account>> = db.collection(ACCOUNTS_COLLECTION)
        .whereEqualTo(ACCOUNT_DOCUMENT_OWNER_ID, currentUser.id)
        .watchAsFlow { value ->
            value.documents.mapNotNull {
                it.toAccountOrThrow(currentUser)
            }
        }

    override fun watchPendingInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>> =
        watchPendingInvitations(currentUser)
            .flatMapToAccounts(currentUser)

    override suspend fun sendInvitationToAccount(
        currentUser: CurrentUser,
        account: Account,
        invitedUserEmail: String
    ) {
        val accountRef = db.collection(ACCOUNTS_COLLECTION).document(account.id)
        val invitationRef = db.collection(INVITATIONS_COLLECTION).document()

        db.runTransaction { transaction ->
            transaction.update(accountRef, mapOf(
                ACCOUNT_DOCUMENT_MEMBERS to FieldValue.arrayUnion(invitedUserEmail),
            ))

            transaction.set(invitationRef, mapOf(
                INVITATION_DOCUMENT_ACCOUNT_ID to account.id,
                INVITATION_DOCUMENT_STATUS to InvitationStatus.SENT.dbValue,
                INVITATION_DOCUMENT_RECEIVER_EMAIL to invitedUserEmail,
                INVITATION_DOCUMENT_SENDER_ID to currentUser.id,
                INVITATION_DOCUMENT_SENDER_EMAIL to currentUser.email,
            ))
        }.await()
    }

    override suspend fun acceptInvitationToAccount(currentUser: CurrentUser, account: Account) {
        updateInvitationStatus(currentUser, account, InvitationStatus.ACCEPTED)
    }

    override suspend fun rejectInvitationToAccount(currentUser: CurrentUser, account: Account) {
        updateInvitationStatus(currentUser, account, InvitationStatus.REJECTED)
    }

    private suspend fun updateInvitationStatus(currentUser: CurrentUser, account: Account, status: InvitationStatus) {
        val invitationResult = db.collection(INVITATIONS_COLLECTION)
            .whereEqualTo(INVITATION_DOCUMENT_RECEIVER_EMAIL, currentUser.email)
            .whereEqualTo(INVITATION_DOCUMENT_ACCOUNT_ID, account.id)
            .get()
            .await()

        if (invitationResult.isEmpty || invitationResult.documents.isEmpty()) {
            throw IllegalStateException("Unable to find invitation")
        }

        db.collection(ACCOUNTS_COLLECTION)
            .document(invitationResult.documents.first().id)
            .update(mapOf(
                INVITATION_DOCUMENT_STATUS to status.dbValue,
            ))
            .await()
    }

    private fun watchInvitedAccounts(currentUser: CurrentUser): Flow<List<Account>> =
        watchAcceptedInvitations(currentUser)
            .flatMapToAccounts(currentUser)

    private fun Flow<List<String>>.flatMapToAccounts(currentUser: CurrentUser): Flow<List<Account>>
        = flatMapLatest { accountIds ->
            if (accountIds.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            return@flatMapLatest db.collection(ACCOUNTS_COLLECTION)
                .whereArrayContains(ACCOUNT_DOCUMENT_MEMBERS, currentUser.email)
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

    private fun watchPendingInvitations(currentUser: CurrentUser): Flow<List<String>> = db.collection(INVITATIONS_COLLECTION)
        .whereEqualTo(INVITATION_DOCUMENT_RECEIVER_EMAIL, currentUser.email)
        .whereEqualTo(INVITATION_DOCUMENT_STATUS, InvitationStatus.SENT.dbValue)
        .watchAsFlow { value ->
            value.documents.mapNotNull {
                it.getString(INVITATION_DOCUMENT_ACCOUNT_ID)
            }
        }

    private fun <T> Query.watchAsFlow(processData: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val snapshotListener = addSnapshotListener { value, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (value == null) {
                close(IllegalStateException("Missing value for snapshot listener"))
                return@addSnapshotListener
            }

            val data = try {
                processData(value)
            } catch (e: Exception) {
                close(e)
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

    private fun DocumentSnapshot.toInvitationOrThrow(): Invitation {
        return Invitation(
            id = id,
            senderEmail = getString(INVITATION_DOCUMENT_SENDER_EMAIL)!!,
            senderId = getString(INVITATION_DOCUMENT_SENDER_ID)!!,
            receiverEmail = getString(INVITATION_DOCUMENT_RECEIVER_EMAIL)!!,
            accountId = getString(INVITATION_DOCUMENT_ACCOUNT_ID)!!,
            status = InvitationStatus.values().first {
                it.dbValue.toLong() == getLong(INVITATION_DOCUMENT_STATUS)
            },
        )
    }

    private fun generateSecureAccountIdAndSecret(): Pair<String, String> {
        val accountId = generateRandomString(100)
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

    companion object {
        private const val INVITATIONS_COLLECTION = "invitations"
        private const val INVITATION_DOCUMENT_RECEIVER_EMAIL = "receiverEmail"
        private const val INVITATION_DOCUMENT_STATUS = "status"
        private const val INVITATION_DOCUMENT_ACCOUNT_ID = "accountId"
        private const val INVITATION_DOCUMENT_SENDER_ID = "senderId"
        private const val INVITATION_DOCUMENT_SENDER_EMAIL = "senderEmail"

        private const val ACCOUNTS_COLLECTION = "accounts"
        private const val ACCOUNT_DOCUMENT_SECRET = "secret"
        private const val ACCOUNT_DOCUMENT_OWNER_ID = "ownerId"
        private const val ACCOUNT_DOCUMENT_OWNER_EMAIL = "ownerEmail"
        private const val ACCOUNT_DOCUMENT_NAME = "name"
        private const val ACCOUNT_DOCUMENT_MEMBERS = "members"

        private const val SECURE_STRING_ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}