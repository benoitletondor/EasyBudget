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

package com.benoitletondor.easybudgetapp.view.main

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.model.Account
import com.benoitletondor.easybudgetapp.accounts.model.AccountCredentials
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.config.Config
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.RestoreAction
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.AppModule
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.parameters.ONBOARDING_STEP_COMPLETED
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getInitDate
import com.benoitletondor.easybudgetapp.parameters.getLastBackupDate
import com.benoitletondor.easybudgetapp.parameters.getLatestSelectedOnlineAccountId
import com.benoitletondor.easybudgetapp.parameters.getOnboardingStep
import com.benoitletondor.easybudgetapp.parameters.isBackupEnabled
import com.benoitletondor.easybudgetapp.parameters.setLatestSelectedOnlineAccountId
import com.benoitletondor.easybudgetapp.parameters.setUserSawMonthlyReportHint
import com.benoitletondor.easybudgetapp.parameters.watchFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.watchLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.watchShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.parameters.watchUserSawMonthlyReportHint
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.kotlin.mongodb.exceptions.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iab: Iab,
    private val parameters: Parameters,
    private val accounts: Accounts,
    private val auth: Auth,
    private val dbProvider: CurrentDBProvider,
    config: Config,
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private val selectedOnlineAccountIdMutableStateFlow: MutableStateFlow<String?>
        = MutableStateFlow(parameters.getLatestSelectedOnlineAccountId())

    val alertMessageFlow = iab.iabStatusFlow.flatMapLatest { iabStatus ->
        when(iabStatus) {
            PremiumCheckStatus.PRO_SUBSCRIBED -> config.watchProAlertMessage()
                .flatMapLatest { maybeProAlertMessage ->
                    if (maybeProAlertMessage != null) {
                        flowOf(maybeProAlertMessage)
                    } else {
                        config.watchGlobalAlertMessage()
                    }
                }
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED,
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING,
            PremiumCheckStatus.ERROR -> config.watchGlobalAlertMessage()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val showMonthlyReportHintFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .flatMapLatest {
            iabStatus -> when(iabStatus) {
                PremiumCheckStatus.PRO_SUBSCRIBED,
                PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                PremiumCheckStatus.LEGACY_PREMIUM -> parameters.watchUserSawMonthlyReportHint()
                    .map { !it }
                else -> flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val selectedDateMutableStateFlow = MutableStateFlow(LocalDate.now())
    val selectedDateFlow: StateFlow<LocalDate> = selectedDateMutableStateFlow

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    private val forceRefreshMutableFlow = MutableLiveFlow<Unit>()
    val forceRefreshFlow: Flow<Unit> = forceRefreshMutableFlow

    private val recurringExpenseDeletionProgressStateMutableFlow = MutableStateFlow<RecurringExpenseDeleteProgressState>(
        RecurringExpenseDeleteProgressState.Idle)
    val recurringExpenseDeletionProgressStateFlow: StateFlow<RecurringExpenseDeleteProgressState> = recurringExpenseDeletionProgressStateMutableFlow

    private val recurringExpenseRestoreProgressStateMutableFlow = MutableStateFlow<RecurringExpenseRestoreProgressState>(
        RecurringExpenseRestoreProgressState.Idle)
    val recurringExpenseRestoreProgressStateFlow: StateFlow<RecurringExpenseRestoreProgressState> = recurringExpenseRestoreProgressStateMutableFlow

    private val showGoToCurrentMonthButtonStateMutableFlow = MutableStateFlow(false)
    val showGoToCurrentMonthButtonStateFlow: StateFlow<Boolean> = showGoToCurrentMonthButtonStateMutableFlow

    private val retryLoadingAccountsEventMutableFlow = MutableSharedFlow<Unit>()
    private val retryLoadingDBEventMutableFlow = MutableSharedFlow<Unit>()

    val includeCheckedBalanceFlow = iab.iabStatusFlow
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

    val hasPendingInvitationsFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .flatMapLatest { iabStatus ->
            when(iabStatus) {
                PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                    .flatMapLatest { authState ->
                        when(authState) {
                            is AuthState.Authenticated -> accounts.watchHasPendingInvitedAccounts(authState.currentUser)
                            AuthState.Authenticating,
                            AuthState.NotAuthenticated -> flowOf(false)
                        }
                    }
                else -> flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val accountSelectionFlow: StateFlow<SelectedAccount> = combine(
        selectedOnlineAccountIdMutableStateFlow,
        iab.iabStatusFlow,
        iab.iabStatusFlow
            .flatMapLatest { iabStatus ->
                when(iabStatus) {
                    PremiumCheckStatus.PRO_SUBSCRIBED -> auth.state
                        .flatMapLatest { authState ->
                            when(authState) {
                                is AuthState.Authenticated -> accounts.watchAccounts(authState.currentUser)
                                    .map { OnlineAccountResponse.Available(it) }
                                AuthState.Authenticating,
                                AuthState.NotAuthenticated -> flowOf(OnlineAccountResponse.Loading)
                            }
                        }
                    else -> flowOf(OnlineAccountResponse.Loading)
                }
            }
    ) { selectedOnlineAccountId, iabStatus, onlineAccountsResponse ->
        if (selectedOnlineAccountId == null) {
            return@combine SelectedAccount.Selected.Offline
        }

        return@combine when(iabStatus) {
            PremiumCheckStatus.INITIALIZING,
            PremiumCheckStatus.CHECKING -> SelectedAccount.Loading
            PremiumCheckStatus.ERROR -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.NOT_PREMIUM,
            PremiumCheckStatus.LEGACY_PREMIUM,
            PremiumCheckStatus.PREMIUM_SUBSCRIBED -> SelectedAccount.Selected.Offline
            PremiumCheckStatus.PRO_SUBSCRIBED -> when(onlineAccountsResponse) {
                is OnlineAccountResponse.Available -> onlineAccountsResponse.accounts
                    .firstOrNull { it.id == selectedOnlineAccountId }
                    ?.let { account ->
                        SelectedAccount.Selected.Online(
                            name = account.name,
                            isOwner = account.isUserOwner,
                            ownerEmail = account.ownerEmail,
                            accountId = account.id,
                            accountSecret = account.secret,
                        )
                    } ?: SelectedAccount.Selected.Offline
                OnlineAccountResponse.Loading -> SelectedAccount.Loading
            }
        }
    }.retryWhen { cause, _ ->
        Logger.error("Error while building accountSelectionFlow", cause)
        emit(SelectedAccount.Selected.Offline)

        retryLoadingAccountsEventMutableFlow.first()

        true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectedAccount.Loading)

    private var changesWatchingJob: Job? = null
    val dbAvailableFlow: StateFlow<DBState> = accountSelectionFlow
        .flatMapLatest { selectedAccount ->
            changesWatchingJob?.cancel()

            when(selectedAccount) {
                SelectedAccount.Loading -> flowOf(DBState.NotLoaded)
                is SelectedAccount.Selected -> flow<DBState> {
                    emit(DBState.Loading)
                    val db = withContext(Dispatchers.IO) {
                        when(selectedAccount) {
                            SelectedAccount.Selected.Offline -> AppModule.provideDB(appContext)
                            is SelectedAccount.Selected.Online -> {
                                val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser ?: throw IllegalStateException("User is not authenticated")

                                AppModule.provideSyncedOnlineDBOrThrow(
                                    currentUser = currentUser,
                                    accountId = selectedAccount.accountId,
                                    accountSecret = selectedAccount.accountSecret,
                                )
                            }
                        }
                    }

                    changesWatchingJob = viewModelScope.launch {
                        db.onChangeFlow
                            .collect {
                                forceRefreshMutableFlow.emit(Unit)
                            }
                    }

                    dbProvider.activeDB = db

                    emit(DBState.Loaded(db))
                }
            }
        }
        .retryWhen { cause, _ ->
            Logger.error("Error while loading DB", cause)
            emit(DBState.Error(cause))

            retryLoadingDBEventMutableFlow.first()
            emit(DBState.Loading)

            if (cause is AuthException) {
                try {
                    Logger.debug("Refreshing user tokens")
                    auth.refreshUserTokens()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Logger.error("Error while force refreshing user token", e)
                }
            }

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DBState.NotLoaded)

    val selectedDateDataFlow = combine(
        dbAvailableFlow.filterIsInstance<DBState.Loaded>(),
        selectedDateMutableStateFlow,
        includeCheckedBalanceFlow,
        forceRefreshMutableFlow
            .onStart {
                emit(Unit)
            },
    ) { _, date, includeCheckedBalance, _ ->
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

        SelectedDateExpensesData.DataAvailable(date, balance, checkedBalance, expenses) as SelectedDateExpensesData
    }
        .catch { e ->
            Logger.error("Error while getting selected date data", e)
            emit(SelectedDateExpensesData.NoDataAvailable)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SelectedDateExpensesData.NoDataAvailable)

    fun onDiscoverPremiumButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenPremium)
        }
    }

    fun onCurrentAccountTapped() {
        viewModelScope.launch {
            retryLoadingAccountsEventMutableFlow.emit(Unit)
            eventMutableFlow.emit(Event.ShowAccountSelect)
        }
    }

    fun onAccountSelected(account: SelectedAccount.Selected) {
        viewModelScope.launch {
            retryLoadingAccountsEventMutableFlow.emit(Unit)
        }

        val onlineAccountId = when(account) {
            SelectedAccount.Selected.Offline -> null
            is SelectedAccount.Selected.Online -> account.accountId
        }

        parameters.setLatestSelectedOnlineAccountId(onlineAccountId)
        selectedOnlineAccountIdMutableStateFlow.value = onlineAccountId
    }

    val showMenuActionButtonsFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .map { iab.isIabReady() }
        .distinctUntilChanged()
        .flatMapLatest { isIabReady ->
            if (!isIabReady) {
                flowOf(false)
            } else {
                accountSelectionFlow.map { it is SelectedAccount.Selected }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showPremiumRelatedButtonsFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .map { status ->
            when(status) {
                PremiumCheckStatus.INITIALIZING,
                PremiumCheckStatus.CHECKING,
                PremiumCheckStatus.ERROR,
                PremiumCheckStatus.NOT_PREMIUM -> false
                PremiumCheckStatus.LEGACY_PREMIUM,
                PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                PremiumCheckStatus.PRO_SUBSCRIBED -> true
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showExpensesCheckBoxFlow: StateFlow<Boolean> = showPremiumRelatedButtonsFlow

    val showManageAccountMenuItemFlow: StateFlow<Boolean> = combine(dbAvailableFlow, accountSelectionFlow) { dbState, accountSelection ->
        when(dbState) {
            DBState.NotLoaded,
            DBState.Loading,
            is DBState.Error -> false
            is DBState.Loaded -> when(accountSelection) {
                SelectedAccount.Loading -> false
                is SelectedAccount.Selected -> accountSelection is SelectedAccount.Selected.Online
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val firstDayOfWeekFlow = parameters.watchFirstDayOfWeek()

    val lowMoneyAmountWarningFlow = parameters.watchLowMoneyWarningAmount()

    val userCurrencyFlow = parameters.watchUserCurrency()

    val appInitDate: LocalDate get() = parameters.getInitDate() ?: LocalDate.now()

    val shouldNavigateToOnboarding get() = parameters.getOnboardingStep() != ONBOARDING_STEP_COMPLETED

    init {
        monitorLastBackupState()
    }

    // TODO remove this whole block once we have enough data
    private fun monitorLastBackupState() {
        viewModelScope.launch {
            try {
                iab.iabStatusFlow.collectLatest { iabStatusFlow ->
                    when(iabStatusFlow) {
                        PremiumCheckStatus.INITIALIZING,
                        PremiumCheckStatus.CHECKING,
                        PremiumCheckStatus.ERROR,
                        PremiumCheckStatus.NOT_PREMIUM -> Unit
                        PremiumCheckStatus.LEGACY_PREMIUM,
                        PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                        PremiumCheckStatus.PRO_SUBSCRIBED -> {
                            if (parameters.isBackupEnabled()) {
                                fun backupDiffDays(lastBackupDate: Date?): Long? {
                                    if (lastBackupDate == null) {
                                        return null
                                    }

                                    val now = Date()
                                    val diff = now.time - lastBackupDate.time
                                    val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

                                    return diffInDays
                                }

                                val lastBackupDate = parameters.getLastBackupDate()
                                val backupDiffDaysValue = backupDiffDays(lastBackupDate)
                                if (backupDiffDaysValue != null) {
                                    Logger.warning("Backup is late, last backup was $backupDiffDaysValue days ago", Exception("Late backup exception"))
                                } else {
                                    Logger.warning("Backup is active but never happened")
                                }
                            } else {
                                Logger.debug("Backup is inactive")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.warning("Error while monitoring late offline account backup", e)
            }
        }
    }

    fun onOnboardingResult(onboardingResult: OnboardingResult) {
        viewModelScope.launch {
            if (onboardingResult.onboardingCompleted) {
                // We do this because the onboarding is using an injected DB directly that isn't the one
                // we currently use so we're not getting update events
                (dbAvailableFlow.value as? DBState.Loaded)?.db?.forceCacheWipe()
                forceRefreshMutableFlow.emit(Unit)
            } else {
                eventMutableFlow.emit(Event.CloseApp)
            }
        }

    }

    suspend fun getDataForMonth(month: YearMonth): DataForMonth = awaitDB().getDataForMonth(month)

    fun onRetryLoadingDBButtonPressed() {
        viewModelScope.launch {
            retryLoadingDBEventMutableFlow.emit(Unit)
        }
    }

    fun onMonthlyReportButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenMonthlyReport)
        }
    }

    fun onEditExpensePressed(expense: Expense) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenEditExpense(expense))
        }
    }

    fun onEditRecurringExpenseOccurenceAndFollowingOnesPressed(expense: Expense) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenEditRecurringExpenseOccurrenceAndFollowingOnes(expense))
        }
    }

    fun onEditRecurringExpenseOccurencePressed(expense: Expense) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenEditRecurringExpenseOccurrence(expense))
        }
    }

    fun onMonthlyReportHintDismissed() {
        parameters.setUserSawMonthlyReportHint()
    }

    fun onDeleteExpenseClicked(expense: Expense) {
        viewModelScope.launch {
            try {
                val restoreAction = withContext(Dispatchers.IO) {
                    awaitDB().deleteExpense(expense)
                }

                eventMutableFlow.emit(
                    Event.ExpenseDeletionSuccess(
                        ExpenseDeletionSuccessData(
                            expense,
                            restoreAction,
                        )
                    )
                )
            } catch (t: Throwable) {
                Logger.error("Error while deleting expense", t)
                eventMutableFlow.emit(Event.ExpenseDeletionError(expense))
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
                    eventMutableFlow.emit(Event.RecurringExpenseDeletionResult(RecurringExpenseDeletionEvent.ErrorRecurringExpenseDeleteNotAssociated(expense)))
                    return@launch
                }

                val firstOccurrenceError = withContext(Dispatchers.Default) {
                    deleteType == RecurringExpenseDeleteType.TO && !awaitDB().hasExpensesForRecurringExpenseBeforeDate(associatedRecurringExpense.recurringExpense, expense.date)
                }

                if ( firstOccurrenceError ) {
                    eventMutableFlow.emit(Event.RecurringExpenseDeletionResult(RecurringExpenseDeletionEvent.ErrorCantDeleteBeforeFirstOccurrence(expense)))
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
                    eventMutableFlow.emit(Event.RecurringExpenseDeletionResult(RecurringExpenseDeletionEvent.ErrorIO(expense)))
                    return@launch
                }

                eventMutableFlow.emit(Event.RecurringExpenseDeletionResult(RecurringExpenseDeletionEvent.Success(associatedRecurringExpense.recurringExpense, restoreAction)))
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
                eventMutableFlow.emit(Event.RecurringExpenseRestoreResult(RecurringExpenseRestoreEvent.Success(recurringExpense)))
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                eventMutableFlow.emit(Event.RecurringExpenseRestoreResult(RecurringExpenseRestoreEvent.ErrorIO(recurringExpense)))
            } finally {
                recurringExpenseRestoreProgressStateMutableFlow.value = RecurringExpenseRestoreProgressState.Idle
            }
        }
    }

    fun onAdjustCurrentBalanceClicked() {
        viewModelScope.launch {
            val balance = withContext(Dispatchers.Default) {
                -awaitDB().getBalanceForDay(LocalDate.now())
            }

            eventMutableFlow.emit(Event.StartCurrentBalanceEditor(balance))
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

                    eventMutableFlow.emit(Event.CurrentBalanceEditionSuccess(BalanceAdjustedData(newExpense, diff, newBalance)))
                } else { // If no adjust balance yet, create a new one
                    val persistedExpense = withContext(Dispatchers.Default) {
                        awaitDB().persistExpense(Expense(balanceExpenseTitle, -diff, LocalDate.now(), true))
                    }

                    eventMutableFlow.emit(Event.CurrentBalanceEditionSuccess(BalanceAdjustedData(persistedExpense, diff, newBalance)))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while editing balance", e)
                eventMutableFlow.emit(Event.CurrentBalanceEditionError(e))
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
                if (e is CancellationException) throw e

                Logger.error("Error while restoring balance", e)
                eventMutableFlow.emit(Event.CurrentBalanceRestorationError(e))
            }
        }
    }

    fun onSelectDate(date: LocalDate) {
        selectedDateMutableStateFlow.value = date
    }

    fun onDateLongClicked(date: LocalDate) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenAddExpense(date))
        }
    }

    fun onExpenseChecked(expense: Expense, checked: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    awaitDB().persistExpense(expense.copy(checked = checked))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while checking expense", e)
                eventMutableFlow.emit(Event.ExpenseCheckingError(e))
            }
        }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        showGoToCurrentMonthButtonStateMutableFlow.value = yearMonth != YearMonth.now()
    }

    fun onGoBackToCurrentMonthButtonPressed() {
        viewModelScope.launch {
            selectedDateMutableStateFlow.value = LocalDate.now()
            eventMutableFlow.emit(Event.GoBackToCurrentMonth)
        }
    }

    fun onCheckAllPastEntriesPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowConfirmCheckAllPastEntries)
        }
    }

    fun onSettingsButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowSettings)
        }
    }

    fun onCheckAllPastEntriesConfirmPressed() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    awaitDB().markAllEntriesAsChecked(LocalDate.now())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Logger.error("Error while checking all past entries", e)
                eventMutableFlow.emit(Event.CheckAllPastEntriesError(e))
            }
        }
    }

    fun onManageAccountButtonPressed() {
        viewModelScope.launch {
            (accountSelectionFlow.value as? SelectedAccount.Selected.Online)?.let {
                eventMutableFlow.emit(Event.OpenManageAccount(it))
            }
        }
    }

    fun onAddRecurringEntryPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenAddRecurringExpense(selectedDateFlow.value))
        }
    }

    fun onAddEntryPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenAddExpense(selectedDateFlow.value))
        }
    }

    fun onExpensePressed(expense: Expense) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowExpenseEditionOptions(expense))
        }
    }

    fun onExpenseLongPressed(expense: Expense) {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.ShowExpenseEditionOptions(expense))
        }
    }

    override fun onCleared() {
        val currentDB = dbProvider.activeDB
        val dbState = dbAvailableFlow.value as? DBState.Loaded
        if (currentDB != null && dbState != null && dbState.db == currentDB) {
            dbProvider.activeDB = null
        }

        try {
            (dbState?.db as? OnlineDB)?.close()
        } catch (e: Exception) {
            Logger.warning("Error while trying to close online DB when clearing, continuing")
        }

        changesWatchingJob?.cancel()

        super.onCleared()
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

    private sealed class OnlineAccountResponse {
        data object Loading : OnlineAccountResponse()
        data class Available(val accounts: List<Account>) : OnlineAccountResponse()
    }

    sealed class SelectedAccount {
        data object Loading : SelectedAccount()
        sealed class Selected : SelectedAccount() {
            data object Offline : Selected()
            @Immutable
            data class Online(
                val name: String,
                val isOwner: Boolean,
                val ownerEmail: String,
                val accountId: String,
                val accountSecret: String,
            ) : Selected() {
                fun toAccountCredentials() = AccountCredentials(
                    id = accountId,
                    secret = accountSecret,
                )
            }
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

    sealed class Event {
        data object ShowAccountSelect : Event()
        data object ShowSettings : Event()
        data object OpenPremium : Event()
        data object GoBackToCurrentMonth : Event()
        data class ExpenseDeletionSuccess(val data: ExpenseDeletionSuccessData) : Event()
        data class ExpenseDeletionError(val expense: Expense) : Event()
        data class RecurringExpenseDeletionResult(val data: RecurringExpenseDeletionEvent) : Event()
        data class RecurringExpenseRestoreResult(val data: RecurringExpenseRestoreEvent) : Event()
        data class StartCurrentBalanceEditor(val currentBalance: Double) : Event()
        data class CurrentBalanceEditionError(val error: Exception) : Event()
        data class CurrentBalanceEditionSuccess(val data: BalanceAdjustedData) : Event()
        data class CurrentBalanceRestorationError(val error: Exception) : Event()
        data class ExpenseCheckingError(val error: Exception) : Event()
        data object ShowConfirmCheckAllPastEntries : Event()
        data class CheckAllPastEntriesError(val error: Throwable) : Event()
        data object OpenMonthlyReport : Event()
        data class OpenAddRecurringExpense(val date: LocalDate) : Event()
        data class OpenAddExpense(val date: LocalDate) : Event()
        data class OpenManageAccount(val account: SelectedAccount.Selected.Online) : Event()
        data class ShowExpenseEditionOptions(val expense: Expense) : Event()
        data class OpenEditExpense(val expense: Expense) : Event()
        data class OpenEditRecurringExpenseOccurrenceAndFollowingOnes(val expense: Expense) : Event()
        data class OpenEditRecurringExpenseOccurrence(val expense: Expense) : Event()
        data object StartOnboarding : Event()
        data object CloseApp : Event()
    }


    sealed class SelectedDateExpensesData {
        data object NoDataAvailable : SelectedDateExpensesData()
        @Immutable
        data class DataAvailable(val date: LocalDate, val balance: Double, val checkedBalance: Double?, val expenses: List<Expense>) : SelectedDateExpensesData()
    }

    data class ExpenseDeletionSuccessData(val deletedExpense: Expense, val restoreAction: RestoreAction)
    data class BalanceAdjustedData(val balanceExpense: Expense, val diffWithOldBalance: Double, val newBalance: Double)

    private suspend fun awaitDB() = dbAvailableFlow.filterIsInstance<DBState.Loaded>().first().db

    sealed class DBState {
        data object NotLoaded : DBState()
        data object Loading : DBState()
        @Immutable
        class Loaded(val db: DB) : DBState()
        class Error(val error: Throwable) : DBState()
    }
}