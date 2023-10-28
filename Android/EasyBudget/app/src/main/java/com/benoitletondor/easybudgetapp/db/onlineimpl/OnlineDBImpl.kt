/*
 *   Copyright 2023 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.log.RealmLogger
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.subscriptions
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.notifications.UpdatedRealm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class OnlineDBImpl(
    private val realm: Realm,
    override val account: Account,
    private val app: App,
) : OnlineDB, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private var recurringExpenseWatchingJob: Job? = null
    private var changesWatchingJob: Job? = null

    private val syncSessionState = MutableStateFlow<SyncSessionState>(SyncSessionState.NotStarted)
    private val recurringExpensesLoadingStateMutableFlow = MutableStateFlow<RecurringExpenseLoadingState>(RecurringExpenseLoadingState.NotLoaded)

    init {
        Logger.debug("Opening Online DB: ${account.id}")

        watchAllRecurringExpenses()
        watchAllChanges()
    }

    private val onChangeMutableFlow = MutableSharedFlow<Unit>()

    override val onChangeFlow: Flow<Unit> = onChangeMutableFlow

    override fun ensureDBCreated() { /* No-op */ }

    override suspend fun triggerForceWriteToDisk() { /* No-op */ }

    suspend fun awaitSyncDone(): SyncSessionState {
        if (syncSessionState.value is SyncSessionState.Done) {
            return SyncSessionState.Done
        }

        if (syncSessionState.value is SyncSessionState.Started) {
            return syncSessionState.first { it is SyncSessionState.Done || it is SyncSessionState.Error }
        }

        syncSessionState.value = SyncSessionState.Started

        if (!realm.subscriptions.waitForSynchronization(timeout = 10.toDuration(DurationUnit.SECONDS)) ) {
            syncSessionState.value = SyncSessionState.Error(
                realm.subscriptions.errorMessage?.let { TimeoutException(it) } ?: TimeoutException("Unknown error")
            )
        } else {
            syncSessionState.value = SyncSessionState.Done
        }

        return syncSessionState.value
    }

    private fun watchAllRecurringExpenses() {
        recurringExpenseWatchingJob?.cancel()
        recurringExpenseWatchingJob = launch {
            recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Loading

            awaitSyncDone()

            realm.query<RecurringExpenseEntity>(account.generateQuery())
                .asFlow()
                .catch { e ->
                    recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Error(e)
                }
                .collect { changes ->
                    recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Loaded(changes.list)
                }
        }
    }

    private fun watchAllChanges() {
        changesWatchingJob?.cancel()
        changesWatchingJob = launch {
            realm.asFlow()
                .filter { it is UpdatedRealm }
                .debounce(500)
                .collect {
                    onChangeMutableFlow.emit(Unit)
                }
        }
    }

    private suspend fun awaitRecurringExpensesLoadOrThrow(): RecurringExpenseLoadingState.Loaded {
        suspend fun awaitResultsWithTimeout(): RecurringExpenseLoadingState.Loaded {
            return withTimeout(readWriteTimeout) {
                recurringExpensesLoadingStateMutableFlow
                    .filterIsInstance<RecurringExpenseLoadingState.Loaded>()
                    .first()
            }
        }

        suspend fun reloadAndAwaitResultsWithTimeout(): RecurringExpenseLoadingState.Loaded {
            watchAllRecurringExpenses()
            return awaitResultsWithTimeout()
        }

        return when(val state = recurringExpensesLoadingStateMutableFlow.value) {
            is RecurringExpenseLoadingState.Error ->  reloadAndAwaitResultsWithTimeout()
            is RecurringExpenseLoadingState.Loaded -> state
            RecurringExpenseLoadingState.Loading -> awaitResultsWithTimeout()
            RecurringExpenseLoadingState.NotLoaded -> reloadAndAwaitResultsWithTimeout()
        }
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        if (expense.associatedRecurringExpense != null) {
            val recurringExpenseEntity = recurringExpenses.firstOrNull { it._id == expense.associatedRecurringExpense.recurringExpense.id }
                ?: throw IllegalStateException("Unable to persist exception for recurring expense id ${expense.associatedRecurringExpense.recurringExpense.id}, not found")

            return realm.write {
                findLatest(recurringExpenseEntity)?.addExceptionFromExpense(
                    expense = expense,
                    originalOccurrenceDate = expense.associatedRecurringExpense.originalDate,
                )

                return@write expense
            }
        } else {
            return realm.write {
                val entity = ExpenseEntity.fromExpense(expense, account)

                val persistedExpense = copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
                return@write persistedExpense.toExpense(associatedRecurringExpense = null)
            }
        }
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val hasRecurringExpensesThisDay = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .any { it.isNotEmpty() }

        if (hasRecurringExpensesThisDay) {
            return true
        }

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDay(dayDate)}")
            .limit(1)
            .count()
            .asFlow()
            .first()

        return expenses > 0
    }

    override suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val hasRecurringExpensesOfTheDayChecked = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .flatten()
            .any { !it.checked }

        if (hasRecurringExpensesOfTheDayChecked) {
            return true
        }

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDay(dayDate)} AND checked == false")
            .limit(1)
            .count()
            .asFlow()
            .first()

        return expenses > 0
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpensesOfTheDay = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .flatten()

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDay(dayDate)}")
            .asFlow()
            .first()
            .list
            .map { it.toExpense(associatedRecurringExpense = null) }

        return recurringExpensesOfTheDay + expenses
    }

    override suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense> {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        val monthEndDate = monthStartDate.plusMonths(1).minusDays(1)

        val recurringExpensesOfTheDay = recurringExpenses
            .map { it.generateExpenses(monthStartDate, monthEndDate) }
            .flatten()

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDateRange(monthStartDate, monthEndDate)}")
            .asFlow()
            .first()
            .list
            .map { it.toExpense(associatedRecurringExpense = null) }

        return recurringExpensesOfTheDay + expenses
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val sumOfRecurringExpenseUpToTheDay = recurringExpenses
            .map { it.generateExpenses(from = null, to = dayDate) }
            .flatten()
            .map { it.amount }
            .fold(0.0) { acc, expenseAmount -> acc + expenseAmount }

        val expensesSumUpToTheDay = realm.query<ExpenseEntity>("${account.generateQuery()} AND date <= ${dayDate.toEpochDay()}")
            .sum("amount", Long::class)
            .asFlow()
            .first()
            .let { it.getRealValueFromDB() }

        return sumOfRecurringExpenseUpToTheDay + expensesSumUpToTheDay
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val sumOfRecurringCheckedExpenseUpToTheDay = recurringExpenses
            .asSequence()
            .map { it.generateExpenses(from = null, to = dayDate) }
            .flatten()
            .filter { it.checked }
            .map { it.amount }
            .fold(0.0) { acc, expenseAmount -> acc + expenseAmount }

        val checkedExpensesSumUpToTheDay = realm.query<ExpenseEntity>("${account.generateQuery()} AND date <= ${dayDate.toEpochDay()} AND checked == true")
            .sum("amount", Long::class)
            .asFlow()
            .first()
            .let { it.getRealValueFromDB() }

        return sumOfRecurringCheckedExpenseUpToTheDay + checkedExpensesSumUpToTheDay
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        awaitRecurringExpensesLoadOrThrow()

        return realm.write {
            val entity = RecurringExpenseEntity.newFromRecurringExpense(recurringExpense, account)

            val persistedEntity = copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
            return@write recurringExpense.copy(
                id = persistedEntity._id,
            )
        }
    }

    override suspend fun updateRecurringExpenseAfterDate(
        newRecurringExpense: RecurringExpense,
        oldOccurrenceDate: LocalDate,
    ) {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = newRecurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it._id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")

        realm.write {
            findLatest(entity)?.updateAllOccurrencesAfterDate(
                oldOccurrenceDate,
                newRecurringExpense,
            )
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val id = recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense without id")
        val entity = recurringExpenses.firstOrNull { it._id == id } ?: throw IllegalStateException("Deleting recurring expense but can't find it for id: $id")

        realm.write {
            findLatest(entity)?.let { delete(it) }
        }

        return {
            realm.write {
                copyToRealm(RecurringExpenseEntity(
                    id = null,
                    representation = entity.iCalRepresentation,
                    account = account,
                ), UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun deleteExpense(expense: Expense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        val expenseId = expense.id ?: throw IllegalStateException("Try to delete an expense without id")

        if (expense.associatedRecurringExpense != null) {
            val recurringExpenseId = expense.associatedRecurringExpense.recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense occurrence without id")
            val entity = recurringExpenses.firstOrNull { it._id == recurringExpenseId } ?: throw IllegalStateException("Deleting recurring expense occurrence but can't find it for id: $recurringExpenseId")
            val icalBeforeDeletion = entity.iCalRepresentation

            realm.write {
                findLatest(entity)?.deleteOccurrence(expense.date)
            }

            return {
                realm.write {
                    findLatest(entity)?.let {
                        it.iCalRepresentation = icalBeforeDeletion
                    }
                }
            }
        } else {
            val expenseEntity: ExpenseEntity = realm.query<ExpenseEntity>("${account.generateQuery()} AND _id == $expenseId").find().first()

            realm.write {
                findLatest(expenseEntity)?.let { delete(it) }
            }

            return {
                realm.write {
                    val entityToRestore = ExpenseEntity.fromExpense(expense, account)
                    copyToRealm(entityToRestore, UpdatePolicy.ALL)
                }
            }
        }
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it._id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        realm.write {
            findLatest(entity)?.deleteOccurrencesAfterDate(afterDate)
        }

        return {
            realm.write {
                findLatest(entity)?.let {
                    it.iCalRepresentation = icalBeforeEdit
                }
            }
        }
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it._id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        realm.write {
            findLatest(entity)?.deleteOccurrencesBeforeDate(beforeDate)
        }

        return {
            realm.write {
                findLatest(entity)?.let {
                    it.iCalRepresentation = icalBeforeEdit
                }
            }
        }
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it._id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")

        return entity.getFirstOccurrenceDate().isBefore(beforeDate)
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        return recurringExpenses.firstOrNull { it._id == recurringExpenseId }?.toRecurringExpense()
    }

    override suspend fun getOldestExpense(): Expense? {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val oldestRecurringExpenseOccurrence = recurringExpenses
            .map { it.getFirstOccurrence() }.minByOrNull { it.date }

        val oldestExpense = realm.query<ExpenseEntity>(account.generateQuery())
            .sort("date")
            .limit(1)
            .asFlow()
            .first()
            .list
            .map { it.toExpense(associatedRecurringExpense = null) }
            .firstOrNull()

        return if (oldestExpense != null && oldestRecurringExpenseOccurrence != null) {
            if (oldestExpense.date.isBefore(oldestRecurringExpenseOccurrence.date)) {
                oldestExpense
            } else {
                oldestRecurringExpenseOccurrence
            }
        } else {
            oldestExpense ?: oldestRecurringExpenseOccurrence
        }
    }

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate) {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND date < ${beforeDate.toEpochDay()}")
            .asFlow()
            .first()
            .list

        realm.write {
            for(expense in expenses) {
                findLatest(expense)?.let {
                    it.checked = true
                }
            }
        }

        for (recurringExpense in recurringExpenses) {
            realm.write {
                findLatest(recurringExpense)?.markAllOccurrencesAsChecked(beforeDate)
            }
        }
    }

    override suspend fun deleteAllEntries() {
        realm.write {
            val expenses = query<ExpenseEntity>(account.generateQuery()).find()
            val recurringExpenses = query<RecurringExpenseEntity>(account.generateQuery()).find()

            delete(expenses)
            delete(recurringExpenses)
        }
    }

    override fun close() {
        if (realm.isClosed()) {
            Logger.debug("Ignoring call to close for Online DB: ${account.id}, already closed")
            return
        }

        Logger.debug("Closing Online DB: ${account.id}")
        realm.close()
        app.close()
        cancel()
    }

    private fun generateQueryForDateRange(from: LocalDate, to: LocalDate): String
        = "date >= ${from.toEpochDay()} AND date <= ${to.toEpochDay()}"

    private fun generateQueryForDay(day: LocalDate): String
        = "date == ${day.toEpochDay()}"

    sealed class SyncSessionState {
        data object NotStarted : SyncSessionState()
        data object Started : SyncSessionState()
        data object Done : SyncSessionState()
        class Error(val exception: Throwable) : SyncSessionState()
    }

    private sealed class RecurringExpenseLoadingState {
        data object NotLoaded : RecurringExpenseLoadingState()
        data object Loading : RecurringExpenseLoadingState()
        data class Loaded(val expenses: List<RecurringExpenseEntity>) : RecurringExpenseLoadingState()
        class Error(val exception: Throwable) : RecurringExpenseLoadingState()
    }

    companion object {
        private val readWriteTimeout = 5.toDuration(DurationUnit.SECONDS)

        suspend fun provideFor(
            atlasAppId: String,
            currentUser: CurrentUser,
            accountId: String,
            accountSecret: String,
        ): OnlineDBImpl {
            val app = App.create(atlasAppId)

            val user = withContext(Dispatchers.IO) {
                try {
                    app.login(Credentials.jwt(currentUser.token))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    val appCurrentUser = app.currentUser
                    if (appCurrentUser != null && appCurrentUser.loggedIn && appCurrentUser.identities.firstOrNull()?.id == currentUser.id) {
                        Logger.warning("Error while authenticating to Realm, using cached user", e)
                        appCurrentUser
                    } else {
                        throw e
                    }
                }
            }

            val account = Account(accountId, accountSecret)
            val realm = Realm.open(
                SyncConfiguration.Builder(
                    user = user,
                    schema = setOf(ExpenseEntity::class, RecurringExpenseEntity::class),
                )
                .name("${account.id}.realm")
                .initialSubscriptions(rerunOnOpen = true) { realm ->
                    add(
                        query = realm.query<ExpenseEntity>(account.generateQuery()),
                        name = "${currentUser.id}:${account.id}:expenses",
                    )
                    add(
                        query = realm.query<RecurringExpenseEntity>(account.generateQuery()),
                        name = "${currentUser.id}:${account.id}:recurring",
                    )
                }
                .log(level = if (BuildConfig.DEBUG_LOG) LogLevel.INFO else LogLevel.WARN, listOf(
                    object : RealmLogger {
                        override val level: LogLevel = LogLevel.WARN
                        override val tag: String = "EasyBudgetAtlas"

                        override fun log(
                            level: LogLevel,
                            throwable: Throwable?,
                            message: String?,
                            vararg args: Any?
                        ) {
                            val argsString = args
                                .mapNotNull {
                                    it?.toString()
                                }
                                .joinToString { ", " }

                            when (level) {
                                LogLevel.WARN -> {
                                    Logger.warning((message ?: "Realm warning") + " $argsString", throwable)
                                }
                                LogLevel.ERROR -> {
                                    Logger.error((message ?: "Realm error") + " $argsString", throwable)
                                }

                                else -> Unit // No-op
                            }
                        }

                    }
                ))
                .build()
            )

            val db = OnlineDBImpl(
                realm,
                account,
                app,
            )

            db.awaitSyncDone()

            return db
        }
    }
}