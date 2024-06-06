/*
 *   Copyright 2024 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.main.account

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.watchShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.account.AccountFragment.Companion.ARG_SELECTED_ACCOUNT
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.kotlin.mongodb.exceptions.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val parameters: Parameters,
    private val iab: Iab,
    private val auth: Auth,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
): ViewModel() {
    private val account = savedStateHandle.get<MainViewModel.SelectedAccount.Selected>(ARG_SELECTED_ACCOUNT)
        ?: throw IllegalStateException("No ARG_SELECTED_ACCOUNT arg")

    private val dbAvailableMutableStateFlow = MutableStateFlow<DBState>(DBState.Loading)
    val dbAvailableFlow: StateFlow<DBState> = dbAvailableMutableStateFlow

    val premiumStatusFlow: StateFlow<PremiumCheckStatus> = iab.iabStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PremiumCheckStatus.INITIALIZING)

    val shouldShowPremiumRelatedButtons: Boolean get() = when(iab.iabStatusFlow.value) {
        PremiumCheckStatus.INITIALIZING,
        PremiumCheckStatus.CHECKING,
        PremiumCheckStatus.ERROR,
        PremiumCheckStatus.NOT_PREMIUM -> false
        PremiumCheckStatus.LEGACY_PREMIUM,
        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
        PremiumCheckStatus.PRO_SUBSCRIBED -> true
    }

    private val selectDateMutableStateFlow = MutableStateFlow(LocalDate.now())
    val selectDateFlow: StateFlow<LocalDate> = selectDateMutableStateFlow

    private val goBackToCurrentMonthEventMutableFlow = MutableSharedFlow<Unit>()
    val goBackToCurrentMonthEventFlow: Flow<Unit> = goBackToCurrentMonthEventMutableFlow

    private val expenseDeletionSuccessEventMutableFlow = MutableLiveFlow<ExpenseDeletionSuccessData>()
    val expenseDeletionSuccessEventFlow: Flow<ExpenseDeletionSuccessData> = expenseDeletionSuccessEventMutableFlow

    private val expenseDeletionErrorEventMutableFlow = MutableLiveFlow<Expense>()
    val expenseDeletionErrorEventFlow: Flow<Expense> = expenseDeletionErrorEventMutableFlow

    private val recurringExpenseDeletionProgressStateMutableFlow = MutableStateFlow<RecurringExpenseDeleteProgressState>(RecurringExpenseDeleteProgressState.Idle)
    val recurringExpenseDeletionProgressStateFlow: Flow<RecurringExpenseDeleteProgressState> = recurringExpenseDeletionProgressStateMutableFlow

    private val recurringExpenseDeletionEventMutableFlow = MutableLiveFlow<RecurringExpenseDeletionEvent>()
    val recurringExpenseDeletionEventFlow: Flow<RecurringExpenseDeletionEvent> = recurringExpenseDeletionEventMutableFlow

    private val recurringExpenseRestoreProgressStateMutableFlow = MutableStateFlow<RecurringExpenseRestoreProgressState>(
        RecurringExpenseRestoreProgressState.Idle)
    val recurringExpenseRestoreProgressStateFlow: Flow<RecurringExpenseRestoreProgressState> = recurringExpenseRestoreProgressStateMutableFlow

    private val recurringExpenseRestoreEventMutableFlow = MutableLiveFlow<RecurringExpenseRestoreEvent>()
    val recurringExpenseRestoreEventFlow: Flow<RecurringExpenseRestoreEvent> = recurringExpenseRestoreEventMutableFlow

    private val startCurrentBalanceEditorEventMutableFlow = MutableLiveFlow<Double>()
    val startCurrentBalanceEditorEventFlow: Flow<Double> = startCurrentBalanceEditorEventMutableFlow

    private val showGoToCurrentMonthButtonStateMutableFlow = MutableStateFlow(false)
    val showGoToCurrentMonthButtonStateFlow: StateFlow<Boolean> = showGoToCurrentMonthButtonStateMutableFlow

    private val currentBalanceEditingErrorEventMutableFlow = MutableLiveFlow<Exception>()
    val currentBalanceEditingErrorEventFlow: Flow<Exception> = currentBalanceEditingErrorEventMutableFlow

    private val currentBalanceEditedEventMutableFlow = MutableLiveFlow<BalanceAdjustedData>()
    val currentBalanceEditedEventFlow: Flow<BalanceAdjustedData> = currentBalanceEditedEventMutableFlow

    private val currentBalanceRestoringErrorEventMutableFlow = MutableLiveFlow<Exception>()
    val currentBalanceRestoringErrorEventFlow: Flow<Exception> = currentBalanceRestoringErrorEventMutableFlow

    private val expenseCheckedErrorEventMutableFlow = MutableLiveFlow<Exception>()
    val expenseCheckedErrorEventFlow: Flow<Exception> = expenseCheckedErrorEventMutableFlow

    private val confirmCheckAllPastEntriesEventMutableFlow = MutableLiveFlow<Unit>()
    val confirmCheckAllPastEntriesEventFlow: Flow<Unit> = confirmCheckAllPastEntriesEventMutableFlow

    private val checkAllPastEntriesErrorEventMutableFlow = MutableLiveFlow<Throwable>()
    val checkAllPastEntriesErrorEventFlow: Flow<Throwable> = checkAllPastEntriesErrorEventMutableFlow

    private val openMonthlyReportEventMutableFlow = MutableLiveFlow<Unit>()
    val openMonthlyReportEventFlow: Flow<Unit> = openMonthlyReportEventMutableFlow

    private val openExpenseAddEventMutableFlow = MutableLiveFlow<LocalDate>()
    val openExpenseAddEventFlow: Flow<LocalDate> = openExpenseAddEventMutableFlow

    private val openManageAccountEventMutableFlow = MutableLiveFlow<MainViewModel.SelectedAccount.Selected.Online>()
    val openManageAccountEventFlow: Flow<MainViewModel.SelectedAccount.Selected.Online> = openManageAccountEventMutableFlow

    private val forceRefreshMutableFlow = MutableSharedFlow<Unit>()
    val forceRefreshFlow: Flow<Unit> = forceRefreshMutableFlow

    private val showManageAccountMenuItemMutableFlow = MutableStateFlow(false)
    val showManageAccountMenuItem: StateFlow<Boolean> = showManageAccountMenuItemMutableFlow

    private var changesWatchingJob: Job? = null

    val includeCheckedBalanceFlow = premiumStatusFlow
        .mapNotNull { when(it) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> null
            PremiumCheckStatus.ERROR,
            PremiumCheckStatus.NOT_PREMIUM -> false
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.PRO_SUBSCRIBED -> true
        } }
        .distinctUntilChanged()
        .combine(parameters.watchShouldShowCheckedBalance()) { isPremium, shouldShowCheckedBalance ->
            isPremium && shouldShowCheckedBalance
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val selectedDateDataFlow = combine(
        selectDateMutableStateFlow,
        includeCheckedBalanceFlow,
        forceRefreshMutableFlow
            .onStart {
                emit(Unit)
            },
    ) { date, includeCheckedBalance, _ ->
        val (balance, expenses, checkedBalance) = withContext(Dispatchers.Default) {
            Triple(
                getBalanceForDay(date),
                awaitDB().getExpensesForDay(date),
                if (includeCheckedBalance) {
                    getCheckedBalanceForDay(date)
                } else {
                    null
                },
            )
        }

        SelectedDateExpensesData(date, balance, checkedBalance, expenses)
    }
        .catch { e ->
            Logger.error("Error while getting selected date data", e)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SelectedDateExpensesData(selectDateMutableStateFlow.value, 0.0, null, emptyList()))

    init {
        loadDB()
    }

    private fun loadDB() {
        viewModelScope.launch {
            dbAvailableMutableStateFlow.value = DBState.Loading
            showManageAccountMenuItemMutableFlow.value = false

            try {
                val db = when(account) {
                    MainViewModel.SelectedAccount.Selected.Offline -> AppModule.provideDB(appContext)
                    is MainViewModel.SelectedAccount.Selected.Online -> {
                        val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser ?: throw IllegalStateException("User is not authenticated")

                        val onlineDb = withContext(Dispatchers.IO) {
                            AppModule.provideSyncedOnlineDBOrThrow(
                                currentUser = currentUser,
                                accountId = account.accountId,
                                accountSecret = account.accountSecret,
                            )
                        }

                        showManageAccountMenuItemMutableFlow.value = true
                        onlineDb
                    }
                }

                currentDBRef = WeakReference(db)
                changesWatchingJob?.cancel()
                changesWatchingJob = launch {
                    db.onChangeFlow
                        .collect {
                            forceRefreshMutableFlow.emit(Unit)
                        }
                }

                dbAvailableMutableStateFlow.value = DBState.Loaded(db)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while loading DB", e)

                dbAvailableMutableStateFlow.value = DBState.Error(e)
            }
        }
    }

    override fun onCleared() {
        val currentDB = currentDBRef?.get()
        val dbState = dbAvailableMutableStateFlow.value as? DBState.Loaded
        if (currentDB != null && dbState != null && dbState.db == currentDB) {
            currentDBRef = null
        }

        try {
            (dbState?.db as? OnlineDB)?.close()
        } catch (e: Exception) {
            Logger.warning("Error while trying to close online DB when clearing, continuing")
        }

        super.onCleared()
    }

    fun onRetryLoadingButtonPressed() {
        val e = (dbAvailableMutableStateFlow.value as? DBState.Error)?.error
        if (e != null && e is AuthException) {
            viewModelScope.launch {
                dbAvailableMutableStateFlow.value = DBState.Loading

                try {
                    Logger.debug("Refreshing user tokens")
                    auth.refreshUserTokens()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Logger.error("Error while force refreshing user token", e)
                }

                loadDB()
            }
        } else {
            loadDB()
        }
    }

    fun onMonthlyReportButtonPressed() {
        viewModelScope.launch {
            openMonthlyReportEventMutableFlow.emit(Unit)
        }
    }

    sealed class RecurringExpenseDeleteProgressState {
        data object Idle : RecurringExpenseDeleteProgressState()
        class Deleting(val expense: Expense): RecurringExpenseDeleteProgressState()
    }

    sealed class RecurringExpenseDeletionEvent {
        class ErrorRecurringExpenseDeleteNotAssociated(val expense: Expense): RecurringExpenseDeletionEvent()
        class ErrorCantDeleteBeforeFirstOccurrence(val expense: Expense): RecurringExpenseDeletionEvent()
        class ErrorIO(val expense: Expense): RecurringExpenseDeletionEvent()
        class Success(val recurringExpense: RecurringExpense, val restoreAction: RestoreAction): RecurringExpenseDeletionEvent()
    }

    sealed class RecurringExpenseRestoreProgressState {
        data object Idle : RecurringExpenseRestoreProgressState()
        class Restoring(val recurringExpense: RecurringExpense): RecurringExpenseRestoreProgressState()
    }

    sealed class RecurringExpenseRestoreEvent {
        class ErrorIO(val recurringExpense: RecurringExpense): RecurringExpenseRestoreEvent()
        class Success(val recurringExpense: RecurringExpense): RecurringExpenseRestoreEvent()
    }

    fun onDeleteExpenseClicked(expense: Expense) {
        viewModelScope.launch {
            try {
                val restoreAction = withContext(Dispatchers.IO) {
                    awaitDB().deleteExpense(expense)
                }

                expenseDeletionSuccessEventMutableFlow.emit(ExpenseDeletionSuccessData(
                    expense,
                    restoreAction,
                ))
            } catch (t: Throwable) {
                Logger.error("Error while deleting expense", t)
                expenseDeletionErrorEventMutableFlow.emit(expense)
            }
        }
    }

    fun onExpenseDeletionCancelled(restoreAction: RestoreAction) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    restoreAction()
                }
            } catch (t: Throwable) {
                Logger.error("Error while restoring expense", t)
            }
        }
    }

    fun onDeleteRecurringExpenseClicked(expense: Expense, deleteType: RecurringExpenseDeleteType) {
        viewModelScope.launch {
            recurringExpenseDeletionProgressStateMutableFlow.value = RecurringExpenseDeleteProgressState.Deleting(expense)

            try {
                val associatedRecurringExpense = expense.associatedRecurringExpense
                if( associatedRecurringExpense == null ) {
                    recurringExpenseDeletionEventMutableFlow.emit(RecurringExpenseDeletionEvent.ErrorRecurringExpenseDeleteNotAssociated(expense))
                    return@launch
                }

                val firstOccurrenceError = withContext(Dispatchers.Default) {
                    deleteType == RecurringExpenseDeleteType.TO && !awaitDB().hasExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense.recurringExpense, expense.date)
                }

                if ( firstOccurrenceError ) {
                    recurringExpenseDeletionEventMutableFlow.emit(RecurringExpenseDeletionEvent.ErrorCantDeleteBeforeFirstOccurrence(expense))
                    return@launch
                }

                val restoreAction: RestoreAction? = withContext(Dispatchers.IO) {
                    when (deleteType) {
                        RecurringExpenseDeleteType.ALL -> {
                            try {
                                awaitDB().deleteRecurringExpense(associatedRecurringExpense.recurringExpense)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e

                                Logger.error("Error while deleting recurring expense", e)
                                null
                            }
                        }
                        RecurringExpenseDeleteType.FROM -> {
                            try {
                                awaitDB().deleteAllExpenseForRecurringExpenseAfterDate(associatedRecurringExpense.recurringExpense, expense.date)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e

                                Logger.error("Error while deleting recurring expense from", e)
                                null
                            }
                        }
                        RecurringExpenseDeleteType.TO -> {
                            try {
                                awaitDB().deleteAllExpenseForRecurringExpenseBeforeDate(associatedRecurringExpense.recurringExpense, expense.date)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e

                                Logger.error("Error while deleting recurring expense to", e)
                                null
                            }
                        }
                        RecurringExpenseDeleteType.ONE -> {
                            try {
                                awaitDB().deleteExpense(expense)
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e

                                Logger.error("Error while deleting recurring expense one", e)
                                null
                            }
                        }
                    }
                }

                if( restoreAction == null ) {
                    recurringExpenseDeletionEventMutableFlow.emit(RecurringExpenseDeletionEvent.ErrorIO(expense))
                    return@launch
                }

                recurringExpenseDeletionEventMutableFlow.emit(RecurringExpenseDeletionEvent.Success(associatedRecurringExpense.recurringExpense, restoreAction))
            } finally {
                recurringExpenseDeletionProgressStateMutableFlow.value = RecurringExpenseDeleteProgressState.Idle
            }
        }

    }

    fun onRestoreRecurringExpenseClicked(recurringExpense: RecurringExpense, restoreAction: RestoreAction) {
        viewModelScope.launch {
            recurringExpenseRestoreProgressStateMutableFlow.value = RecurringExpenseRestoreProgressState.Restoring(recurringExpense)

            try {
                restoreAction()
                recurringExpenseRestoreEventMutableFlow.emit(RecurringExpenseRestoreEvent.Success(recurringExpense))
            } catch (e: Exception) {
                recurringExpenseRestoreEventMutableFlow.emit(RecurringExpenseRestoreEvent.ErrorIO(recurringExpense))
            } finally {
                recurringExpenseRestoreProgressStateMutableFlow.value = RecurringExpenseRestoreProgressState.Idle
            }
        }
    }

    fun onIabStatusChanged() {
        viewModelScope.launch {
            forceRefreshMutableFlow.emit(Unit)
        }
    }

    fun onAdjustCurrentBalanceClicked() {
        viewModelScope.launch {
            val balance = withContext(Dispatchers.Default) {
                -awaitDB().getBalanceForDay(LocalDate.now())
            }

            startCurrentBalanceEditorEventMutableFlow.emit(balance)
        }
    }

    fun onNewBalanceSelected(newBalance: Double, balanceExpenseTitle: String) {
        viewModelScope.launch {
            try {
                val currentBalance = withContext(Dispatchers.Default) {
                    -awaitDB().getBalanceForDay(LocalDate.now())
                }

                if (newBalance == currentBalance) {
                    // Nothing to do, balance hasn't change
                    return@launch
                }

                val diff = newBalance - currentBalance

                // Look for an existing balance for the day
                val existingExpense = withContext(Dispatchers.Default) {
                    awaitDB().getExpensesForDay(LocalDate.now()).find { it.title == balanceExpenseTitle }
                }

                if (existingExpense != null) { // If the adjust balance exists, just add the diff and persist it
                    val newExpense = withContext(Dispatchers.Default) {
                        awaitDB().persistExpense(existingExpense.copy(amount = existingExpense.amount - diff))
                    }

                    currentBalanceEditedEventMutableFlow.emit(BalanceAdjustedData(newExpense, diff, newBalance))
                } else { // If no adjust balance yet, create a new one
                    val persistedExpense = withContext(Dispatchers.Default) {
                        awaitDB().persistExpense(Expense(balanceExpenseTitle, -diff, LocalDate.now(), true))
                    }

                    currentBalanceEditedEventMutableFlow.emit(BalanceAdjustedData(persistedExpense, diff, newBalance))
                }
            } catch (e: Exception) {
                Logger.error("Error while editing balance", e)
                currentBalanceEditingErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onCurrentBalanceEditedCancelled(expense: Expense, diff: Double) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if( expense.amount + diff == 0.0 ) {
                        awaitDB().deleteExpense(expense)
                    } else {
                        val newExpense = expense.copy(amount = expense.amount + diff)
                        awaitDB().persistExpense(newExpense)
                    }
                }
            } catch (e: Exception) {
                Logger.error("Error while restoring balance", e)
                currentBalanceRestoringErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onSelectDate(date: LocalDate) {
        selectDateMutableStateFlow.value = date
    }

    fun onDateLongClicked(date: LocalDate) {
        viewModelScope.launch {
            openExpenseAddEventMutableFlow.emit(date)
        }
    }

    fun onCurrencySelected() {
        viewModelScope.launch {
            forceRefreshMutableFlow.emit(Unit)
        }
    }

    private suspend fun getBalanceForDay(date: LocalDate): Double {
        var balance = 0.0 // Just to keep a positive number if balance == 0
        balance -= awaitDB().getBalanceForDay(date)

        return balance
    }

    private suspend fun getCheckedBalanceForDay(date: LocalDate): Double {
        var balance = 0.0 // Just to keep a positive number if balance == 0
        balance -= awaitDB().getCheckedBalanceForDay(date)

        return balance
    }

    fun onExpenseChecked(expense: Expense, checked: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    awaitDB().persistExpense(expense.copy(checked = checked))
                }
            } catch (e: Exception) {
                Logger.error("Error while checking expense", e)
                expenseCheckedErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        showGoToCurrentMonthButtonStateMutableFlow.value = yearMonth != YearMonth.now()
    }

    fun onGoBackToCurrentMonthButtonPressed() {
        viewModelScope.launch {
            selectDateMutableStateFlow.value = LocalDate.now()
            goBackToCurrentMonthEventMutableFlow.emit(Unit)
        }
    }

    fun onShowCheckedBalanceChanged() {
        viewModelScope.launch {
            forceRefreshMutableFlow.emit(Unit)
        }
    }

    fun onCheckAllPastEntriesPressed() {
        viewModelScope.launch {
            confirmCheckAllPastEntriesEventMutableFlow.emit(Unit)
        }
    }

    fun onCheckAllPastEntriesConfirmPressed() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    awaitDB().markAllEntriesAsChecked(LocalDate.now())
                }
            } catch (e: Exception) {
                Logger.error("Error while checking all past entries", e)
                checkAllPastEntriesErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onLowMoneyWarningThresholdChanged() {
        viewModelScope.launch {
            forceRefreshMutableFlow.emit(Unit)
        }
    }

    fun onManageAccountButtonPressed() {
        viewModelScope.launch {
            (account as? MainViewModel.SelectedAccount.Selected.Online)?.let {
                openManageAccountEventMutableFlow.emit(it)
            }
        }
    }

    private suspend fun awaitDB() = dbAvailableMutableStateFlow.filterIsInstance<DBState.Loaded>().first().db

    sealed class DBState {
        data object Loading : DBState()
        class Loaded(val db: DB) : DBState()
        class Error(val error: Exception) : DBState()
    }

    companion object {
        private var currentDBRef: WeakReference<DB>? = null

        fun getCurrentDB(): DB? = currentDBRef?.get()
    }
}

data class SelectedDateExpensesData(val date: LocalDate, val balance: Double, val checkedBalance: Double?, val expenses: List<Expense>)
data class ExpenseDeletionSuccessData(val deletedExpense: Expense, val restoreAction: RestoreAction)
data class BalanceAdjustedData(val balanceExpense: Expense, val diffWithOldBalance: Double, val newBalance: Double)