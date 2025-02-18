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

package com.benoitletondor.easybudgetapp.view.main

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.EasyBudget
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
import com.benoitletondor.easybudgetapp.helper.OfflineAccountBackupStatus
import com.benoitletondor.easybudgetapp.helper.getOfflineAccountBackupStatusFlow
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
import com.benoitletondor.easybudgetapp.parameters.getLatestSelectedOnlineAccountId
import com.benoitletondor.easybudgetapp.parameters.getOnboardingStep
import com.benoitletondor.easybudgetapp.parameters.setLatestSelectedOnlineAccountId
import com.benoitletondor.easybudgetapp.parameters.setUserSawMonthlyReportHint
import com.benoitletondor.easybudgetapp.parameters.watchFirstDayOfWeek
import com.benoitletondor.easybudgetapp.parameters.watchLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.watchShouldShowCheckedBalance
import com.benoitletondor.easybudgetapp.parameters.watchUserSawMonthlyReportHint
import com.benoitletondor.easybudgetapp.view.onboarding.OnboardingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iab: Iab,
    private val parameters: Parameters,
    private val accounts: Accounts,
    private val auth: Auth,
    private val dbProvider: CurrentDBProvider,
    private val offlineDB: DB,
    config: Config,
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private val selectedOnlineAccountIdMutableStateFlow: MutableStateFlow<String?>
        = MutableStateFlow(parameters.getLatestSelectedOnlineAccountId())

    val alertMessageFlow = iab.iabStatusFlow.flatMapLatest { iabStatus ->
        when(iabStatus) {
            PremiumCheckStatus.PRO_SUBSCRIBED -> config.watchProMigratedToPgAlertMessage()
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
    private val retryLoadingSelectedDateDataEventMutableFlow = MutableSharedFlow<Unit>()
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

    val shouldDisplayAccountsWarningFlow: StateFlow<Boolean> = getOfflineAccountBackupStatusFlow(iab, parameters, auth)
        .map { status ->
            when(status) {
                is OfflineAccountBackupStatus.Enabled -> status.authState is AuthState.NotAuthenticated
                is OfflineAccountBackupStatus.Disabled,
                OfflineAccountBackupStatus.Unavailable -> false
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
                                AuthState.Authenticating -> flowOf(OnlineAccountResponse.Loading)
                                AuthState.NotAuthenticated -> flowOf(OnlineAccountResponse.Error("Not authenticated"))
                            }
                        }
                    PremiumCheckStatus.INITIALIZING,
                    PremiumCheckStatus.CHECKING -> flowOf(OnlineAccountResponse.Loading)
                    PremiumCheckStatus.ERROR -> flowOf(OnlineAccountResponse.Error("Error checking Pro status"))
                    PremiumCheckStatus.NOT_PREMIUM,
                    PremiumCheckStatus.LEGACY_PREMIUM,
                    PremiumCheckStatus.PREMIUM_SUBSCRIBED -> flowOf(OnlineAccountResponse.Error("Not a Pro user"))
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
                            hasBeenMigratedToPg = account.hasBeenMigratedToPg,
                        )
                    } ?: SelectedAccount.Selected.Offline
                OnlineAccountResponse.Loading -> SelectedAccount.Loading
                is OnlineAccountResponse.Error -> {
                    Logger.error("Error while loading online accounts", Exception(onlineAccountsResponse.message))
                    SelectedAccount.Selected.Offline
                }
            }
        }
    }
        // This ensures we don't emit a new account and reload everything if only the hasBeenMigratedToPg flag changes
        // Make sure to remove this when migration has been done
        .runningFold(SelectedAccount.Loading as SelectedAccount) { previous, next ->
            if (previous is SelectedAccount.Selected.Online && next is SelectedAccount.Selected.Online) {
                if (previous == next.copy(hasBeenMigratedToPg = false)) {
                    return@runningFold previous
                }
            }

            return@runningFold next
        }
        .retryWhen { cause, _ ->
            Logger.error("Error while building accountSelectionFlow", cause)
            emit(SelectedAccount.Selected.Offline)

            retryLoadingAccountsEventMutableFlow.first()

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SelectedAccount.Loading)

    val dbAvailableFlow: StateFlow<DBState> = EasyBudget.isAppForegroundStateFlow
        .flatMapLatest { isAppForeground ->
            if (!isAppForeground) {
                return@flatMapLatest flowOf(DBState.NotLoaded)
            }

            return@flatMapLatest accountSelectionFlow.flatMapLatest { selectedAccount ->
                when(selectedAccount) {
                    SelectedAccount.Loading -> flowOf(DBState.NotLoaded)
                    is SelectedAccount.Selected -> flow<DBState> {
                        emit(DBState.Loading)
                        val db = withContext(Dispatchers.IO) {
                            when(selectedAccount) {
                                SelectedAccount.Selected.Offline -> offlineDB
                                is SelectedAccount.Selected.Online -> {
                                    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser ?: throw IllegalStateException("User is not authenticated")

                                    AppModule.provideSyncedOnlineDBOrThrow(
                                        appContext = appContext,
                                        currentUser = currentUser,
                                        auth = auth,
                                        accountId = selectedAccount.accountId,
                                        accountSecret = selectedAccount.accountSecret,
                                        accountHasBeenMigratedToPg = selectedAccount.hasBeenMigratedToPg,
                                        accounts = accounts,
                                    )
                                }
                            }
                        }

                        val watchingJob = viewModelScope.launch {
                            db.onChangeFlow
                                .collect {
                                    forceRefreshMutableFlow.emit(Unit)
                                }
                        }

                        emit(DBState.Loaded(db, watchingJob))
                    }
                }
            }
        }
        .runningFold(DBState.NotLoaded as DBState) { previous, next ->
            Logger.debug("Going from DBState.${previous.logName()} to DBState.${next.logName()}")

            // Clean up old state
            (previous as? DBState.Loaded)?.let { loadedState ->
                Logger.debug("Closing old DB & watcher")

                try {
                    loadedState.close()
                } catch (e: Exception) {
                    Logger.warning("Error while trying to close online DB when changing DB state, continuing", e)
                }
            }

            dbProvider.activeDB = when(next) {
                is DBState.Loaded -> next.db
                DBState.Loading,
                DBState.NotLoaded,
                is DBState.Error -> null
            }

            return@runningFold next
        }
        .retryWhen { cause, _ ->
            Logger.error("Error while loading DB", cause)
            emit(DBState.Error(cause))

            retryLoadingDBEventMutableFlow.first()
            emit(DBState.Loading)

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DBState.NotLoaded)

    private fun DBState.logName() = when(this) {
        is DBState.Error -> "Error"
        is DBState.Loaded -> "Loaded (${if (this.db is OnlineDB) { "Online" } else { "Offline" }})"
        DBState.Loading -> "Loading"
        DBState.NotLoaded -> "NotLoaded"
    }

    val selectedDateDataFlow = combine(
        dbAvailableFlow,
        selectedDateMutableStateFlow,
        includeCheckedBalanceFlow,
        forceRefreshMutableFlow
            .onStart {
                emit(Unit)
            },
    ) { dbState, date, includeCheckedBalance, _ ->
        when(dbState) {
            is DBState.Loaded -> {
                val (balance, expenses, checkedBalance) = withContext(Dispatchers.Default) {
                    Triple(
                        dbState.db.computeBalanceForDay(date),
                        dbState.db.getExpensesForDay(date),
                        if (includeCheckedBalance) {
                            dbState.db.computeCheckedBalanceForDay(date)
                        } else {
                            null
                        },
                    )
                }

                SelectedDateExpensesData.DataAvailable(date, balance, checkedBalance, expenses)
            }
            DBState.Loading,
            DBState.NotLoaded -> SelectedDateExpensesData.LoadingData
            is DBState.Error -> SelectedDateExpensesData.ErrorLoadingData(dbState.error)
        }
    }
        .retryWhen { e, _ ->
            Logger.error("Error while getting selected date data", e)
            emit(SelectedDateExpensesData.ErrorLoadingData(e))

            retryLoadingSelectedDateDataEventMutableFlow.first()
            emit(SelectedDateExpensesData.LoadingData)

            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SelectedDateExpensesData.LoadingData)

    fun onRetrySelectedDateDataLoadingButtonPressed() {
        viewModelScope.launch {
            retryLoadingSelectedDateDataEventMutableFlow.emit(Unit)
        }
    }

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
        val loadedDBState = dbAvailableFlow.value as? DBState.Loaded
        if (loadedDBState != null) {
            if (currentDB != null && loadedDBState.db == currentDB) {
                Logger.debug("Clearing active DB in MainViewModel onCleared")
                dbProvider.activeDB = null
            }

            try {
                loadedDBState.close()
            } catch (e: Exception) {
                Logger.warning("Error while trying to close online DB when clearing, continuing", e)
            }
        }

        super.onCleared()
    }

    private suspend fun DB.computeBalanceForDay(date: LocalDate): Double {
        var balance = 0.0 // Just to keep a positive number if balance == 0
        balance -= getBalanceForDay(date)

        return balance
    }

    private suspend fun DB.computeCheckedBalanceForDay(date: LocalDate): Double {
        var balance = 0.0 // Just to keep a positive number if balance == 0
        balance -= getCheckedBalanceForDay(date)

        return balance
    }

    private sealed class OnlineAccountResponse {
        data object Loading : OnlineAccountResponse()
        data class Error(val message: String) : OnlineAccountResponse()
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
                val hasBeenMigratedToPg: Boolean,
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
        data object LoadingData : SelectedDateExpensesData()
        data class ErrorLoadingData(val error: Throwable) : SelectedDateExpensesData()
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
        class Loaded(val db: DB, private val watchingJob: Job) : DBState() {
            fun close() {
                watchingJob.cancel()
                (db as? OnlineDB)?.close()
            }
        }
        class Error(val error: Throwable) : DBState()
    }
}