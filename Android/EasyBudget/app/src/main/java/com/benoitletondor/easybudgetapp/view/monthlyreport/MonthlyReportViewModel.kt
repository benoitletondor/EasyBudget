package com.benoitletondor.easybudgetapp.view.monthlyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.getListOfMonthsAvailableForUser
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.parameters.Parameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@HiltViewModel(assistedFactory = MonthlyReportViewModelFactory::class)
class MonthlyReportViewModel @AssistedInject constructor(
    iab: Iab,
    parameters: Parameters,
    private val currentDBProvider: CurrentDBProvider,
    @Assisted fromNotification: Boolean,
) : ViewModel() {

    val shouldShowExportToCsvButtonFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .map { it == PremiumCheckStatus.PRO_SUBSCRIBED }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userMonthPositionShiftMutableFlow = MutableStateFlow(if (fromNotification) -1 else 0)

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
                        latest = currentMonthPosition + userMonthPositionShift >= months.size - 1,
                    )

                    return@map State.Loaded(months, selectedPosition)
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: Flow<Event> = eventMutableFlow

    fun onExportToCsvButtonPressed() {
        viewModelScope.launch {
            eventMutableFlow.emit(Event.OpenExportToCsvScreen)
        }
    }

    sealed class Event {
        data object OpenExportToCsvScreen : Event()
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(val months: List<YearMonth>, val selectedPosition: MonthlyReportSelectedPosition) : State()
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
    val latest: Boolean,
)