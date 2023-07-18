package com.benoitletondor.easybudgetapp.view.main.account

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import com.benoitletondor.easybudgetapp.view.main.account.AccountFragment.Companion.ARG_SELECTED_ACCOUNT
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val parameters: Parameters,
    private val iab: Iab,
    private val auth: Auth,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
): ViewModel() {
    private val dbAvailableMutableStateFlow = MutableStateFlow<DBState>(DBState.Loading)
    val dbAvailableFlow: StateFlow<DBState> = dbAvailableMutableStateFlow

    val premiumStatusFlow: StateFlow<PremiumCheckStatus> = iab.iabStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PremiumCheckStatus.INITIALIZING)

    val isIabReady: Boolean get() = iab.isIabReady()
    val isUserPremium: Boolean get() = iab.isUserPremium()

    private val selectDateMutableStateFlow = MutableStateFlow(LocalDate.now())

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

    private val goBackToCurrentMonthEventMutableFlow = MutableLiveFlow<Unit>()
    val goBackToCurrentMonthEventFlow: Flow<Unit> = goBackToCurrentMonthEventMutableFlow

    private val confirmCheckAllPastEntriesEventMutableFlow = MutableLiveFlow<Unit>()
    val confirmCheckAllPastEntriesEventFlow: Flow<Unit> = confirmCheckAllPastEntriesEventMutableFlow

    private val checkAllPastEntriesErrorEventMutableFlow = MutableLiveFlow<Throwable>()
    val checkAllPastEntriesErrorEventFlow: Flow<Throwable> = checkAllPastEntriesErrorEventMutableFlow

    private val forceRefreshMutableFlow = MutableSharedFlow<Unit>()
    val refreshDatesFlow: Flow<Unit> = forceRefreshMutableFlow

    val selectedDateDataFlow = combine(
        selectDateMutableStateFlow,
        forceRefreshMutableFlow,
    ) { date, _ ->
        val (balance, expenses, checkedBalance) = withContext(Dispatchers.Default) {
            Triple(
                getBalanceForDay(date),
                awaitDB().getExpensesForDay(date),
                if (parameters.getShouldShowCheckedBalance()) {
                    getCheckedBalanceForDay(date)
                } else {
                    null
                },
            )
        }

        SelectedDateExpensesData(date, balance, checkedBalance, expenses)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectedDateExpensesData(selectDateMutableStateFlow.value, 0.0, null, emptyList()))

    init {
        loadDB()
    }

    override fun onCleared() {
        (dbAvailableMutableStateFlow.value as? DBState.Loaded)?.db?.close()

        super.onCleared()
    }

    private fun loadDB() {
        dbAvailableMutableStateFlow.value = DBState.Loading

        viewModelScope.launch {
            val selectedDB = savedStateHandle.get<MainViewModel.SelectedAccount.Selected>(ARG_SELECTED_ACCOUNT)
                ?: throw IllegalStateException("No ARG_SELECTED_ACCOUNT arg")

            try {
                dbAvailableMutableStateFlow.value = when(selectedDB) {
                    MainViewModel.SelectedAccount.Selected.Offline -> DBState.Loaded(AppModule.provideDB(appContext))
                    is MainViewModel.SelectedAccount.Selected.Online -> {
                        val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser ?: throw IllegalStateException("User is not authenticated")

                        val onlineDb = AppModule.provideSyncedOnlineDBOrThrow(
                            currentUser = currentUser,
                            accountId = selectedDB.accountId,
                            accountSecret = selectedDB.accountSecret,
                        )

                        DBState.Loaded(onlineDb)
                    }
                }

                forceRefreshMutableFlow.emit(Unit)
            } catch (e: Exception) {
                Logger.error("Error while loading DB", e)
                dbAvailableMutableStateFlow.value = DBState.Error(e);
            }
        }
    }

    fun onRetryLoadingButtonPressed() {
        loadDB()
    }

    sealed class RecurringExpenseDeleteProgressState {
        object Idle : RecurringExpenseDeleteProgressState()
        class Deleting(val expense: Expense): RecurringExpenseDeleteProgressState()
    }

    sealed class RecurringExpenseDeletionEvent {
        class ErrorRecurringExpenseDeleteNotAssociated(val expense: Expense): RecurringExpenseDeletionEvent()
        class ErrorCantDeleteBeforeFirstOccurrence(val expense: Expense): RecurringExpenseDeletionEvent()
        class ErrorIO(val expense: Expense): RecurringExpenseDeletionEvent()
        class Success(val recurringExpense: RecurringExpense, val restoreAction: RestoreAction): RecurringExpenseDeletionEvent()
    }

    sealed class RecurringExpenseRestoreProgressState {
        object Idle : RecurringExpenseRestoreProgressState()
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

                val selectedDate = selectDateMutableStateFlow.value
                expenseDeletionSuccessEventMutableFlow.emit(ExpenseDeletionSuccessData(
                    expense,
                    getBalanceForDay(selectedDate),
                    if (parameters.getShouldShowCheckedBalance()) { awaitDB().getCheckedBalanceForDay(selectedDate) } else { null },
                    restoreAction,
                ))

                forceRefreshMutableFlow.emit(Unit)
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
                    restoreAction.restore()
                }

                forceRefreshMutableFlow.emit(Unit)
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
                            } catch (t: Throwable) {
                                null
                            }
                        }
                        RecurringExpenseDeleteType.FROM -> {
                            try {
                                awaitDB().deleteAllExpenseForRecurringExpenseAfterDate(associatedRecurringExpense.recurringExpense, expense.date)
                            } catch (t: Throwable) {
                                null
                            }
                        }
                        RecurringExpenseDeleteType.TO -> {
                            try {
                                awaitDB().deleteAllExpenseForRecurringExpenseBeforeDate(associatedRecurringExpense.recurringExpense, expense.date)
                            } catch (t: Throwable) {
                                null
                            }
                        }
                        RecurringExpenseDeleteType.ONE -> {
                            try {
                                awaitDB().deleteExpense(expense)
                            } catch (t: Throwable) {
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

            forceRefreshMutableFlow.emit(Unit)
        }

    }

    fun onRestoreRecurringExpenseClicked(recurringExpense: RecurringExpense, restoreAction: RestoreAction) {
        viewModelScope.launch {
            recurringExpenseRestoreProgressStateMutableFlow.value = RecurringExpenseRestoreProgressState.Restoring(recurringExpense)

            try {
                restoreAction.restore()
                recurringExpenseRestoreEventMutableFlow.emit(RecurringExpenseRestoreEvent.Success(recurringExpense))
            } catch (e: Exception) {
                recurringExpenseRestoreEventMutableFlow.emit(RecurringExpenseRestoreEvent.ErrorIO(recurringExpense))
            } finally {
                recurringExpenseRestoreProgressStateMutableFlow.value = RecurringExpenseRestoreProgressState.Idle
            }

            forceRefreshMutableFlow.emit(Unit)
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

                forceRefreshMutableFlow.emit(Unit)
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

                forceRefreshMutableFlow.emit(Unit)
            } catch (e: Exception) {
                Logger.error("Error while restoring balance", e)
                currentBalanceRestoringErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onSelectDate(date: LocalDate) {
        selectDateMutableStateFlow.value = date
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

    fun onDayChanged() {
        selectDateMutableStateFlow.value = LocalDate.now()
    }

    fun onExpenseAdded() {
        viewModelScope.launch {
            forceRefreshMutableFlow.emit(Unit)
        }
    }

    fun onExpenseChecked(expense: Expense, checked: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    awaitDB().persistExpense(expense.copy(checked = checked))
                }

                forceRefreshMutableFlow.emit(Unit)
            } catch (e: Exception) {
                Logger.error("Error while checking expense", e)
                expenseCheckedErrorEventMutableFlow.emit(e)
            }
        }
    }

    fun onMonthChanged(month: Int, year: Int) {
        val cal = Calendar.getInstance()
        showGoToCurrentMonthButtonStateMutableFlow.value = cal.get(Calendar.MONTH) != month || cal.get(
            Calendar.YEAR) != year
    }

    fun onGoBackToCurrentMonthButtonPressed() {
        viewModelScope.launch {
            goBackToCurrentMonthEventMutableFlow.emit(Unit)
            selectDateMutableStateFlow.value = LocalDate.now()
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

                forceRefreshMutableFlow.emit(Unit)
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

    private suspend fun awaitDB() = dbAvailableMutableStateFlow.filterIsInstance<DBState.Loaded>().first().db

    sealed class DBState {
        object Loading : DBState()
        class Loaded(val db: DB) : DBState()
        class Error(val error: Exception) : DBState()
    }
}

data class SelectedDateExpensesData(val date: LocalDate, val balance: Double, val checkedBalance: Double?, val expenses: List<Expense>)
data class ExpenseDeletionSuccessData(val deletedExpense: Expense, val newDayBalance: Double, val newCheckedBalance: Double?, val restoreAction: RestoreAction)
data class BalanceAdjustedData(val balanceExpense: Expense, val diffWithOldBalance: Double, val newBalance: Double)