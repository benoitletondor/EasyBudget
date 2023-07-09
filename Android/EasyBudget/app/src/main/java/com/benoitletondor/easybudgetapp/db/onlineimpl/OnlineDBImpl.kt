package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.Account
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.db.restoreAction
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.helper.toStartOfDayDate
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.subscriptions
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.concurrent.TimeoutException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class OnlineDBImpl(
    private val realm: Realm,
    private val account: Account,
) : DB, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private var recurringExpenseWatchingJob: Job? = null

    private val syncSessionState = MutableStateFlow<SyncSessionState>(SyncSessionState.NotStarted)
    private val recurringExpensesLoadingStateMutableFlow = MutableStateFlow<RecurringExpenseLoadingState>(RecurringExpenseLoadingState.NotLoaded)

    init {
        watchAllRecurringExpenses()
    }

    override fun ensureDBCreated() { /* No-op */ }

    override suspend fun triggerForceWriteToDisk() { /* No-op */ }

    private suspend fun awaitSyncDone() {
        if (syncSessionState.value is SyncSessionState.Done) {
            return
        }

        if (syncSessionState.value is SyncSessionState.Started) {
            syncSessionState.first { it is SyncSessionState.Done || it is SyncSessionState.Error }
            return
        }

        syncSessionState.value = SyncSessionState.Started

        if (!realm.subscriptions.waitForSynchronization(timeout = 10.toDuration(DurationUnit.SECONDS)) ) {
            syncSessionState.value = SyncSessionState.Error(
                realm.subscriptions.errorMessage?.let { TimeoutException(it) } ?: TimeoutException("Unknown error")
            )
        } else {
            syncSessionState.value = SyncSessionState.Done
        }
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
            val recurringExpenseEntity = recurringExpenses.firstOrNull { it.id == expense.associatedRecurringExpense.recurringExpense.id }
                ?: throw IllegalStateException("Unable to persist exception for recurring expense id ${expense.associatedRecurringExpense.recurringExpense.id}, not found")

            recurringExpenseEntity.addExceptionFromExpense(
                expense = expense,
                originalOccurrenceDate = expense.associatedRecurringExpense.originalDate
            )

            return realm.write {
                copyToRealm(recurringExpenseEntity, updatePolicy = UpdatePolicy.ALL)
                return@write expense
            }
        } else {
            val entity = ExpenseEntity.fromExpense(expense, account)
            return realm.write {
                val persistedExpense = copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
                return@write persistedExpense.toExpense(associatedRecurringExpense = null)
            }
        }
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpensesOfTheDay = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .flatten()
            .firstOrNull()

        if (recurringExpensesOfTheDay != null) {
            return true
        }

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDateRange(dayDate, dayDate)}")
            .limit(1)
            .count()
            .asFlow()
            .first()

        return expenses > 0
    }

    override suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpensesOfTheDayChecked = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .flatten()
            .firstOrNull { it.checked }

        if (recurringExpensesOfTheDayChecked != null) {
            return true
        }

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDateRange(dayDate, dayDate)} AND checked = true")
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

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND ${generateQueryForDateRange(dayDate, dayDate)}")
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
            .map { it.generateExpenses(LocalDate.MIN, dayDate) }
            .flatten()
            .map { it.amount }
            .reduce { acc, expenseAmount -> acc + expenseAmount }

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
            .map { it.generateExpenses(LocalDate.MIN, dayDate) }
            .flatten()
            .filter { it.checked }
            .map { it.amount }
            .reduce { acc, expenseAmount -> acc + expenseAmount }

        val checkedExpensesSumUpToTheDay = realm.query<ExpenseEntity>("${account.generateQuery()} AND date <= ${dayDate.toEpochDay()} AND checked = true")
            .sum("amount", Long::class)
            .asFlow()
            .first()
            .let { it.getRealValueFromDB() }

        return sumOfRecurringCheckedExpenseUpToTheDay + checkedExpensesSumUpToTheDay
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        awaitRecurringExpensesLoadOrThrow()

        val entity = RecurringExpenseEntity.newFromRecurringExpense(recurringExpense, account)

        return realm.write {
            val persistedEntity = copyToRealm(entity, updatePolicy = UpdatePolicy.ALL)
            return@write recurringExpense.copy(
                id = persistedEntity.id,
            )
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val id = recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense without id")
        val entity = recurringExpenses.firstOrNull { it.id == id } ?: throw IllegalStateException("Deleting recurring expense but can't find it for id: $id")

        realm.write {
            findLatest(entity)?.let { delete(it) }
        }

        return restoreAction {
            realm.write {
                copyToRealm(entity, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun deleteExpense(expense: Expense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        val expenseId = expense.id ?: throw IllegalStateException("Try to delete an expense without id")

        if (expense.associatedRecurringExpense != null) {
            val recurringExpenseId = expense.associatedRecurringExpense.recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense occurrence without id")
            val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Deleting recurring expense occurrence but can't find it for id: $recurringExpenseId")
            val icalBeforeDeletion = entity.iCalRepresentation
            entity.deleteOccurrence(expense.associatedRecurringExpense.originalDate)

            realm.write {
                copyToRealm(entity, UpdatePolicy.ALL)
            }

            return restoreAction {
                entity.iCalRepresentation = icalBeforeDeletion
                realm.write {
                    copyToRealm(entity, UpdatePolicy.ALL)
                }
            }
        } else {
            val expenseEntity: ExpenseEntity = realm.query<ExpenseEntity>("id = $expenseId").find().first()

            realm.write {
                findLatest(expenseEntity)?.let { delete(it) }
            }

            return restoreAction {
                realm.write {
                    copyToRealm(expenseEntity, UpdatePolicy.ALL)
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
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        entity.deleteOccurrencesAfterDate(afterDate)

        realm.write {
            copyToRealm(entity, UpdatePolicy.ALL)
        }

        return restoreAction {
            entity.iCalRepresentation = icalBeforeEdit
            realm.write {
                copyToRealm(entity, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        entity.deleteOccurrencesBeforeDate(beforeDate)

        realm.write {
            copyToRealm(entity, UpdatePolicy.ALL)
        }

        return restoreAction {
            entity.iCalRepresentation = icalBeforeEdit
            realm.write {
                copyToRealm(entity, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")

        return entity.getFirstOccurrenceDate().isBefore(beforeDate)
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        return recurringExpenses.firstOrNull { it.id == recurringExpenseId }?.toRecurringExpense()
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

        val expenses = realm.query<ExpenseEntity>("${account.generateQuery()} AND date < ${beforeDate.toStartOfDayDate()}")
            .asFlow()
            .first()
            .list

        realm.write {
            for(expense in expenses) {
                findLatest(expense)?.checked = true
            }
        }

        for (recurringExpense in recurringExpenses) {
            recurringExpense.markAllOccurrencesAsChecked(beforeDate)

            realm.write {
                copyToRealm(recurringExpense, UpdatePolicy.ALL)
            }
        }
    }

    override fun close() {
        realm.close()
    }

    private fun generateQueryForDateRange(from: LocalDate, to: LocalDate): String
        = "date >= ${from.toEpochDay()} AND date <= ${to.toEpochDay()}"

    private sealed class SyncSessionState {
        object NotStarted : SyncSessionState()
        object Started : SyncSessionState()
        object Done : SyncSessionState()
        class Error(val exception: Throwable) : SyncSessionState()
    }

    private sealed class RecurringExpenseLoadingState {
        object NotLoaded : RecurringExpenseLoadingState()
        object Loading : RecurringExpenseLoadingState()
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
            val user = app.login(Credentials.jwt(currentUser.token))
            val account = Account(accountId, accountSecret)

            val realm = Realm.open(
                SyncConfiguration.Builder(
                    user = user,
                    schema = setOf(ExpenseEntity::class, RecurringExpenseEntity::class, Account::class),
                ).initialSubscriptions { realm ->
                    add(
                        query = realm.query<ExpenseEntity>(account.generateQuery()),
                        name = "${currentUser.id}:${account.id}:expenses",
                    )
                    add(
                        query = realm.query<RecurringExpenseEntity>(account.generateQuery()),
                        name = "${currentUser.id}:${account.id}:recurring",
                    )
                }.build()
            )

            return OnlineDBImpl(
                realm,
                account,
            )
        }
    }
}