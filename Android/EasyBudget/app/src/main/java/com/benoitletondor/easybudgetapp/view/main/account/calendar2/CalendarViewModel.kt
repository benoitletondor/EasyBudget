package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.combineTransformLatest
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val dbAvailableFlow: StateFlow<AccountViewModel.DBState>,
    private val includeCheckedBalance: StateFlow<Boolean>,
    private val selectedDateFlow: StateFlow<LocalDate>,
    private val onDateSelected: (LocalDate) -> Unit,
) : ViewModel() {
    private val displayedMonthMutableStateFlow = MutableStateFlow(selectedDateFlow.value.yearMonth)
    val displayedMonthFlow: StateFlow<YearMonth> = displayedMonthMutableStateFlow

    private val refreshDataMutableFlow = MutableSharedFlow<Unit>()

    val dataForMonthState = combineTransformLatest(
        dbAvailableFlow,
        includeCheckedBalance,
        displayedMonthFlow,
        refreshDataMutableFlow.onStart { emit(Unit) },
    ) { dbState, includeCheckedBalance, month, _ ->
        when(dbState) {
            AccountViewModel.DBState.Loading -> {
                emit(DataForMonthState.Loading)
            }
            is AccountViewModel.DBState.Error -> {
                emit(DataForMonthState.Error(dbState.error))
            }
            is AccountViewModel.DBState.Loaded -> {
                emit(DataForMonthState.Loading)

                try {
                    val dataForMonth = dbState.db.getDataForMonth(month, includeCheckedBalance)
                    emit(DataForMonthState.Loaded(dataForMonth))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Logger.error("Error while loading data for month in calendar", e)
                    emit(DataForMonthState.Error(e))
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = DataForMonthState.Loading)

    fun onMonthChange(yearMonth: YearMonth) {
        displayedMonthMutableStateFlow.value = yearMonth
    }

    fun onRetryButtonClicked() {
        viewModelScope.launch {
            refreshDataMutableFlow.emit(Unit)
        }
    }

    sealed class DataForMonthState {
        data object Loading : DataForMonthState()
        data class Error(val error: Throwable) : DataForMonthState()
        data class Loaded(val dataForMonth: DataForMonth) : DataForMonthState()
    }
}