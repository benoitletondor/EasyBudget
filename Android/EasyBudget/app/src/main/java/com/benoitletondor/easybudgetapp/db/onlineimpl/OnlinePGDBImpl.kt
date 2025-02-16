/*
 *   Copyright 2025 Benoit Letondor
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

import android.content.Context
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.AccountCredentials
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity.expenseEntityTable
import com.benoitletondor.easybudgetapp.db.onlineimpl.pgentity.recurringExpenseEntityTable
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.DataForDay
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.kizitonwose.calendar.core.atStartOfMonth
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.db.schema.Schema
import com.powersync.utils.JsonParam
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import java.time.LocalDate
import java.time.YearMonth
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val syncTimeout = 5.toDuration(DurationUnit.SECONDS)
private val readWriteTimeout = 5.toDuration(DurationUnit.SECONDS)

class OnlinePGDBImpl(
    private val db: PowerSyncDatabase,
    override val account: Account,
    private val accounts: Accounts,
    private var accountHasBeenMigratedToPg: Boolean,
) : OnlineDB, CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {
    private var recurringExpenseWatchingJob: Job? = null
    private var expenseWatchingJob: Job? = null

    private val recurringExpensesLoadingStateMutableFlow = MutableStateFlow<RecurringExpenseLoadingState>(RecurringExpenseLoadingState.NotLoaded)

    private val onChangeMutableFlow = MutableSharedFlow<Unit>()
    override val onChangeFlow: Flow<Unit> = onChangeMutableFlow

    init {
        Logger.debug("Opening PG Online DB: ${account.id}")

        watchAllRecurringExpenses()
        watchAllExpenses()
    }

    override suspend fun deleteAllEntries() {
        db.writeTransaction { transaction ->
            transaction.execute("DELETE FROM expense", emptyList())
            transaction.execute("DELETE FROM recurring_expense", emptyList())
        }
    }

    override fun ensureDBCreated() {
        // No-op
    }

    override suspend fun triggerForceWriteToDisk() {
        // No-op
    }

    override suspend fun forceCacheWipe() {
        // No-op
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        markAccountAsMigratedIfNeeded()

        if (expense.associatedRecurringExpense != null) {
            val recurringExpenseEntity = recurringExpenses.firstOrNull { it.id == expense.associatedRecurringExpense.recurringExpense.id }
                ?: throw IllegalStateException("Unable to persist exception for recurring expense id ${expense.associatedRecurringExpense.recurringExpense.id}, not found")

            val calRepresentationBeforeUpdate = recurringExpenseEntity.iCalRepresentation
            recurringExpenseEntity.addExceptionFromExpense(
                expense = expense,
                originalOccurrenceDate = expense.associatedRecurringExpense.originalDate,
            )

            try {
                db.writeTransaction { transaction ->
                    recurringExpenseEntity.persistOrThrow(transaction)
                }
            } catch (e: Exception) {
                recurringExpenseEntity.iCalRepresentation = calRepresentationBeforeUpdate
                throw e
            }

            return expense
        } else {
            if (expense.id != null) {
                val expenseEntity = ExpenseEntity.fromExpense(expense.id, expense, account)

                db.writeTransaction { transaction ->
                    expenseEntity.persistOrThrow(transaction)
                }

                return expense
            } else {
                val expenseEntity = db.writeTransaction { transaction ->
                    ExpenseEntity.createFromExpenseOrThrow(SecureRandom().nextLong(), expense, account, transaction)
                }

                return expenseEntity.toExpense(associatedRecurringExpense = null)
            }
        }
    }

    override suspend fun getDataForMonth(yearMonth: YearMonth): DataForMonth {
        val startDate = yearMonth.atStartOfMonth().minusDays(DataForMonth.NUMBER_OF_LEEWAY_DAYS)
        val endDate = yearMonth.atEndOfMonth().plusDays(DataForMonth.NUMBER_OF_LEEWAY_DAYS)

        var balance = getBalanceForDay(startDate.minusDays(1))
        var checkedBalance = getCheckedBalanceForDay(startDate.minusDays(1))

        val expenses = getExpensesBetweenDates(startDate, endDate)
        val daysData = mutableMapOf<LocalDate, DataForDay>()

        var dayDate = startDate
        while (!dayDate.isAfter(endDate)) {
            val dayData = computeDataForDay(dayDate, expenses, balance, checkedBalance)

            daysData[dayDate] = dayData
            balance = dayData.balance
            checkedBalance = dayData.checkedBalance

            dayDate = dayDate.plusDays(1)
        }

        return DataForMonth(
            month = yearMonth,
            daysData = daysData,
        )
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpensesOfTheDay = recurringExpenses
            .map { it.generateExpenses(dayDate, dayDate) }
            .flatten()

        val expenses = db.getAll(
            "SELECT * FROM expense WHERE date = ? ORDER by CASE WHEN created_at IS NULL THEN 'Z' ELSE created_at END ASC",
            listOf(dayDate.toEpochDay()),
            ExpenseEntity::fromCursorOrThrow,
        ).map {
            it.toExpense(associatedRecurringExpense = null)
        }

        return recurringExpensesOfTheDay + expenses
    }

    override suspend fun getExpensesForMonth(month: YearMonth): List<Expense> {
        return getExpensesBetweenDates(month.atStartOfMonth(), month.atEndOfMonth())
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val sumOfRecurringExpenseUpToTheDay = recurringExpenses
            .map { it.generateExpenses(from = null, to = dayDate) }
            .flatten()
            .map { it.amount }
            .fold(0.0) { acc, expenseAmount -> acc + expenseAmount }

        val expensesSumUpToTheDay = db.getOptional(
            "SELECT SUM(amount) FROM expense WHERE date <= ?",
            listOf(dayDate.toEpochDay()),
        ) { cursor ->
            cursor.getLong(0) ?: 0
        }.getRealValueFromDB()

        return sumOfRecurringExpenseUpToTheDay + expensesSumUpToTheDay
    }

    override suspend fun getCheckedBalanceForDay(dayDate: LocalDate): Double {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val sumOfRecurringCheckedExpenseUpToTheDay = recurringExpenses
            .map { it.generateExpenses(from = null, to = dayDate) }
            .flatten()
            .filter { it.checked }
            .map { it.amount }
            .fold(0.0) { acc, expenseAmount -> acc + expenseAmount }

        val checkedExpensesSumUpToTheDay = db.get(
            "SELECT SUM(amount) FROM expense WHERE date <= ? AND checked == 1",
            listOf(dayDate.toEpochDay()),
        ) { cursor ->
            cursor.getLong(0) ?: 0
        }.getRealValueFromDB()

        return sumOfRecurringCheckedExpenseUpToTheDay + checkedExpensesSumUpToTheDay
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        awaitRecurringExpensesLoadOrThrow()

        markAccountAsMigratedIfNeeded()

        val entity = db.writeTransaction { transaction ->
             RecurringExpenseEntity.createFromRecurringExpenseOrThrow(SecureRandom().nextLong(), recurringExpense, account, transaction)
        }

        return entity.toRecurringExpense()
    }

    override suspend fun updateRecurringExpenseAfterDate(
        newRecurringExpense: RecurringExpense,
        oldOccurrenceDate: LocalDate
    ) {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        markAccountAsMigratedIfNeeded()

        val recurringExpenseId = newRecurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")

        val calRepresentationBeforeUpdate = entity.iCalRepresentation
        entity.updateAllOccurrencesAfterDate(
            afterDate = oldOccurrenceDate,
            newRecurringExpense,
        )

        try {
            db.writeTransaction { transaction ->
                entity.persistOrThrow(transaction)
            }
        } catch (e: Exception) {
            entity.iCalRepresentation = calRepresentationBeforeUpdate
            throw e
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        markAccountAsMigratedIfNeeded()

        val id = recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense without id")
        val entity = recurringExpenses.firstOrNull { it.id == id } ?: throw IllegalStateException("Deleting recurring expense but can't find it for id: $id")

        db.writeTransaction { transaction ->
            transaction.execute("DELETE FROM recurring_expense WHERE id = ?", listOf(entity.id))
        }

        return {
            db.writeTransaction { transaction ->
                RecurringExpenseEntity.createFromRecurringExpenseOrThrow(entity.id, recurringExpense, account, transaction)
            }
        }
    }

    override suspend fun deleteExpense(expense: Expense): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses
        val expenseId = expense.id ?: throw IllegalStateException("Try to delete an expense without id")

        markAccountAsMigratedIfNeeded()

        if (expense.associatedRecurringExpense != null) {
            val recurringExpenseId = expense.associatedRecurringExpense.recurringExpense.id ?: throw IllegalStateException("Deleting recurring expense occurrence without id")
            val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Deleting recurring expense occurrence but can't find it for id: $recurringExpenseId")
            val icalBeforeDeletion = entity.iCalRepresentation

            entity.deleteOccurrence(expense.date, expense.associatedRecurringExpense.originalDate)

            try {
                db.writeTransaction { transaction ->
                    entity.persistOrThrow(transaction)
                }
            } catch (e: Exception) {
                entity.iCalRepresentation = icalBeforeDeletion
                throw e
            }

            return {
                entity.iCalRepresentation = icalBeforeDeletion

                db.writeTransaction { transaction ->
                    entity.persistOrThrow(transaction)
                }
            }
        } else {
            db.writeTransaction { transaction ->
                transaction.execute("DELETE FROM expense WHERE id = ?", listOf(expenseId))
            }

            return {
                db.writeTransaction { transaction ->
                    ExpenseEntity.createFromExpenseOrThrow(expense.id, expense, account, transaction)
                }
            }
        }
    }

    override suspend fun deleteAllExpenseForRecurringExpenseAfterDate(
        recurringExpense: RecurringExpense,
        afterDate: LocalDate
    ): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        markAccountAsMigratedIfNeeded()

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        entity.deleteOccurrencesAfterDate(afterDate)

        try {
            db.writeTransaction { transaction ->
                entity.persistOrThrow(transaction)
            }
        } catch (e: Exception) {
            entity.iCalRepresentation = icalBeforeEdit
            throw e
        }

        return {
            db.writeTransaction { transaction ->
                entity.iCalRepresentation = icalBeforeEdit
                entity.persistOrThrow(transaction)
            }
        }
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): RestoreAction {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        markAccountAsMigratedIfNeeded()

        val recurringExpenseId = recurringExpense.id ?: throw IllegalStateException("Editing recurring expense occurrence without id")
        val entity = recurringExpenses.firstOrNull { it.id == recurringExpenseId } ?: throw IllegalStateException("Editing recurring expense occurrence but can't find it for id: $recurringExpenseId")
        val icalBeforeEdit = entity.iCalRepresentation

        entity.deleteOccurrencesBeforeDate(beforeDate)

        try {
            db.writeTransaction { transaction ->
                entity.persistOrThrow(transaction)
            }
        } catch (e: Exception) {
            entity.iCalRepresentation = icalBeforeEdit
            throw e
        }

        return {
            db.writeTransaction { transaction ->
                entity.iCalRepresentation = icalBeforeEdit
                entity.persistOrThrow(transaction)
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

        val oldestExpense = db.getOptional(
            "SELECT * FROM expense ORDER BY date ASC LIMIT 1",
            emptyList(),
            ExpenseEntity::fromCursorOrThrow,
        )?.toExpense(associatedRecurringExpense = null)

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

        markAccountAsMigratedIfNeeded()

        db.writeTransaction { transaction ->
            transaction.execute("UPDATE expense SET checked = 1 WHERE date < ?", listOf(beforeDate.toEpochDay()))

            for (recurringExpense in recurringExpenses) {
                runBlocking { recurringExpense.markAllOccurrencesAsChecked(beforeDate) }
                recurringExpense.persistOrThrow(transaction)
            }
        }
    }

    override fun close() {
        if (!db.currentStatus.connected && !db.currentStatus.connecting) {
            Logger.debug("Ignoring call to close for PG Online DB: ${account.id}, already closed")
            return
        }

        Logger.debug("Closing PG Online DB: ${account.id}")
        runBlocking { db.close() }

        cancel()
        recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.NotLoaded
    }

    private suspend fun getExpensesBetweenDates(start: LocalDate, end: LocalDate): List<Expense> {
        val recurringExpenses = awaitRecurringExpensesLoadOrThrow().expenses

        val recurringExpensesOfTheDay = recurringExpenses
            .map { it.generateExpenses(start, end) }
            .flatten()

        val expenses = db.getAll(
            "SELECT * FROM expense WHERE date >= ? AND date <= ? ORDER by CASE WHEN created_at IS NULL THEN 'Z' ELSE created_at END ASC",
            listOf(start.toEpochDay(), end.toEpochDay()),
            ExpenseEntity::fromCursorOrThrow,
        ).map {
            it.toExpense(associatedRecurringExpense = null)
        }

        return recurringExpensesOfTheDay + expenses
    }

    private fun computeDataForDay(
        dayDate: LocalDate,
        expensesForMonth: List<Expense>,
        balanceBeforeDay: Double,
        checkedBalanceBeforeDay: Double,
    ): DataForDay {
        val expensesForDay = expensesForMonth.filter { it.date == dayDate }

        return DataForDay(
            day = dayDate,
            expenses = expensesForDay,
            balance = balanceBeforeDay + expensesForDay.sumOf { it.amount },
            checkedBalance = checkedBalanceBeforeDay + expensesForDay.filter { it.checked }.sumOf { it.amount },
        )
    }

    private suspend fun awaitSyncDone() {
        withTimeoutOrNull(syncTimeout) {
            db.waitForFirstSync()
        }
    }

    private fun watchAllRecurringExpenses() {
        recurringExpenseWatchingJob?.cancel()
        recurringExpenseWatchingJob = launch {
            recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Loading

            awaitSyncDone()

            db.watch(
                "SELECT * FROM recurring_expense ORDER by CASE WHEN created_at IS NULL THEN 'Z' ELSE created_at END ASC",
                emptyList(),
                RecurringExpenseEntity::fromCursorOrThrow,
            )
                .catch { e ->
                    Logger.error("Error watching recurring expenses", e)
                    recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Error(e)
                }
                .collect { recurringExpenses ->
                    recurringExpensesLoadingStateMutableFlow.value = RecurringExpenseLoadingState.Loaded(recurringExpenses)
                    onChangeMutableFlow.emit(Unit)
                }
        }
    }

    private fun watchAllExpenses() {
        expenseWatchingJob?.cancel()
        expenseWatchingJob = launch {
            awaitSyncDone()

            // This dummy request just adds a watcher on the expense table to trigger a refresh
            // when an expense is edited, both remotely or locally
            db.watch(
                "SELECT null FROM expense LIMIT 1",
                emptyList(),
            ) { _ -> }
                .catch { e ->
                    Logger.error("Fatal error watching expenses!!", e)
                }
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

    // Make sure to delete the runningFold of the MainViewModel when deleting this
    private suspend fun markAccountAsMigratedIfNeeded() {
        if (!accountHasBeenMigratedToPg) {
            Logger.debug("Marking account ${account.id} as migrated to PG")

            accounts.markAccountAsMigratedToPg(
                accountCredentials = AccountCredentials(id = account.id, secret = account.secret),
            )

            accountHasBeenMigratedToPg = true
        }
    }

    private sealed class RecurringExpenseLoadingState {
        data object NotLoaded : RecurringExpenseLoadingState()
        data object Loading : RecurringExpenseLoadingState()
        data class Loaded(val expenses: List<RecurringExpenseEntity>) : RecurringExpenseLoadingState()
        class Error(val exception: Throwable) : RecurringExpenseLoadingState()
    }

    companion object {
        suspend fun provideFor(
            appContext: Context,
            currentUser: CurrentUser,
            auth: Auth,
            accountId: String,
            accountSecret: String,
            accounts: Accounts,
            accountHasBeenMigratedToPg: Boolean,
        ): OnlinePGDBImpl {
            val supabaseConnector = SupabaseConnector(
                supabaseClient = createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
                    builder = {
                        this.accessToken = {
                            (auth.state.value as? AuthState.Authenticated)?.currentUser?.token
                        }

                        install(Postgrest)
                    },
                ),
                powerSyncEndpoint = BuildConfig.POWER_SYNC_ENDPOINT,
                currentUser = currentUser,
                auth = auth,
            )

            val db = PowerSyncDatabase(
                factory = DatabaseDriverFactory(context = appContext),
                schema = Schema(
                    listOf(
                        expenseEntityTable,
                        recurringExpenseEntityTable,
                    )
                ),
                dbFilename = "${accountId}_v1.db",
                logger = co.touchlab.kermit.Logger,
            )

            db.connect(
                supabaseConnector,
                params = mapOf(
                    "account_id" to JsonParam.String(accountId),
                ),
            )

            return OnlinePGDBImpl(
                db = db,
                account = Account(
                    id = accountId,
                    secret = accountSecret,
                ),
                accounts = accounts,
                accountHasBeenMigratedToPg = accountHasBeenMigratedToPg,
            )
        }
    }
}