package com.benoitletondor.easybudgetapp.view.monthlyreport

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.getListOfMonthsAvailableForUser
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.injection.requireDB
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@HiltViewModel(assistedFactory = MonthlyReportViewModelFactory::class)
class MonthlyReportViewModel @AssistedInject constructor(
    iab: Iab,
    private val parameters: Parameters,
    private val currentDBProvider: CurrentDBProvider,
    @Assisted fromNotification: Boolean,
) : ViewModel() {

    val shouldShowExportToCsvButtonFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .map { it == PremiumCheckStatus.PRO_SUBSCRIBED }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userMonthPositionShiftMutableFlow = MutableStateFlow(if (fromNotification) -1 else 0)

    val userCurrencyStateFlow get() = parameters.watchUserCurrency()

    val stateFlow: StateFlow<State> = flow {
            val months = withContext(Dispatchers.IO) {
                parameters.getListOfMonthsAvailableForUser()
            }
            emit(months)
        }
        .flatMapLatest { months ->
            var currentMonthPosition = months.indexOf(YearMonth.now())
            if (currentMonthPosition == -1) {
                Logger.error("Error while getting current month position, returned -1", IllegalStateException("Current month not found in list of available months"))
                currentMonthPosition = months.size - 1
            }

            return@flatMapLatest userMonthPositionShiftMutableFlow
                .map { userMonthPositionShift ->
                    val selectedPosition = MonthlyReportSelectedPosition(
                        position = currentMonthPosition + userMonthPositionShift,
                        month = months[currentMonthPosition + userMonthPositionShift],
                        first = currentMonthPosition + userMonthPositionShift == 0,
                        last = currentMonthPosition + userMonthPositionShift >= months.size - 1,
                    )

                    return@map State.Loaded(months, selectedPosition)
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val retryLoadingMonthDataMutableFlow = MutableSharedFlow<Unit>()

    val monthDataStateFlow: StateFlow<MonthDataState> = stateFlow
        .filterIsInstance<State.Loaded>()
        .map { state ->
            val expensesForMonth = withContext(Dispatchers.Default) {
                currentDBProvider.requireDB.getExpensesForMonth(state.selectedPosition.month)
            }

            if( expensesForMonth.isEmpty() ) {
                return@map MonthDataState.Empty
            }

            val expenses = mutableListOf<Expense>()
            val revenues = mutableListOf<Expense>()
            var revenuesAmount = 0.0
            var expensesAmount = 0.0

            withContext(Dispatchers.Default) {
                for(expense in expensesForMonth) {
                    if( expense.isRevenue() ) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                    }
                }
            }

            return@map MonthDataState.Loaded(expenses, revenues, expensesAmount, revenuesAmount)
        }
        .retryWhen { cause, _ ->
            Logger.error("Error while loading month data", cause)
            emit(MonthDataState.Error(cause))

            retryLoadingMonthDataMutableFlow.first()

            emit(MonthDataState.Loading)
            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MonthDataState.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    fun onExportToCsvButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenExportToCsvScreen)
        }
    }

    fun onRetryLoadingMonthDataPressed() {
        viewModelScope.launch {
            retryLoadingMonthDataMutableFlow.emit(Unit)
        }
    }

    fun onPreviousMonthClicked() {
        val currentState = stateFlow.value
        if (currentState is State.Loaded && currentState.selectedPosition.first) {
            return
        }

        userMonthPositionShiftMutableFlow.value--
    }

    fun onNextMonthClicked() {
        val currentState = stateFlow.value
        if (currentState is State.Loaded && currentState.selectedPosition.last) {
            return
        }

        userMonthPositionShiftMutableFlow.value++
    }

    sealed class Event {
        data object OpenExportToCsvScreen : Event()
    }

    sealed class State {
        data object Loading : State()
        @Immutable
        data class Loaded(val months: List<YearMonth>, val selectedPosition: MonthlyReportSelectedPosition) : State()
    }

    sealed class MonthDataState {
        data object Loading : MonthDataState()
        data object Empty: MonthDataState()
        @Immutable
        data class Error(val error: Throwable) : MonthDataState()
        @Immutable
        data class Loaded(val expenses: List<Expense>, val revenues: List<Expense>, val expensesAmount: Double, val revenuesAmount: Double) : MonthDataState()
    }
}

@AssistedFactory
interface MonthlyReportViewModelFactory {
    fun create(fromNotification: Boolean): MonthlyReportViewModel
}

data class MonthlyReportSelectedPosition(
    val position: Int,
    val month: YearMonth,
    val first: Boolean,
    val last: Boolean,
)