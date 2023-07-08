package com.benoitletondor.easybudgetapp.db.onlineimpl

import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.Account
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.onlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.annotations.ExperimentalFlexibleSyncApi
import io.realm.kotlin.mongodb.ext.subscribe
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.mongodb.sync.WaitForSync
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class OnlineDBImpl(
    private val realm: Realm,
    private val accountId: String,
    private val accountSecret: String,
) : DB, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private val account get() = Account(accountId, accountSecret)

    private var recurringExpenseWatchingJob: Job? = null

    private val recurringExpensesLoadingStateMutableFlow = MutableStateFlow<RecurringExpenseLoadingState>(RecurringExpenseLoadingState.NotLoaded)

    init {
        watchAllRecurringExpenses()
    }

    override fun ensureDBCreated() { /* No-op */ }

    override suspend fun triggerForceWriteToDisk() { /* No-op */ }

    @OptIn(ExperimentalFlexibleSyncApi::class)
    private fun watchAllRecurringExpenses() {
        recurringExpenseWatchingJob?.cancel()
        recurringExpenseWatchingJob = launch {
            recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Loading

            realm.query<RecurringExpenseEntity>(account.generateQuery())
                .subscribe(mode = WaitForSync.FIRST_TIME, timeout = 5.toDuration(DurationUnit.SECONDS))
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
            return withTimeout(5.toDuration(DurationUnit.SECONDS)) {
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
        awaitRecurringExpensesLoadOrThrow()

        TODO("Not yet implemented")
    }

    override suspend fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        TODO("Not yet implemented")
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        TODO("Not yet implemented")
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteExpense(expense: Expense) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpensesForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? {
        TODO("Not yet implemented")
    }

    override suspend fun getOldestExpense(): Expense? {
        TODO("Not yet implemented")
    }

    override suspend fun markAllEntriesAsChecked(beforeDate: LocalDate) {
        TODO("Not yet implemented")
    }

    private sealed class RecurringExpenseLoadingState {
        object NotLoaded : RecurringExpenseLoadingState()
        object Loading : RecurringExpenseLoadingState()
        data class Loaded(val expenses: List<RecurringExpenseEntity>) : RecurringExpenseLoadingState()
        class Error(val exception: Throwable) : RecurringExpenseLoadingState()
    }

    companion object {
        suspend fun provideFor(
            atlasAppId: String,
            currentUser: CurrentUser,
            accountId: String,
            accountSecret: String,
        ): OnlineDBImpl {
            val app = App.create(atlasAppId)
            val user = app.login(Credentials.jwt(currentUser.token))
            val realm = Realm.open(
                SyncConfiguration.Builder(
                    user = user,
                    schema = setOf(ExpenseEntity::class, RecurringExpenseEntity::class, Account::class),
                ).build()
            )

            return OnlineDBImpl(
                realm,
                accountId,
                accountSecret,
            )
        }
    }
}