package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.model.DataForMonth
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val dbAvailableFlow: StateFlow<AccountViewModel.DBState>,
    private val includeCheckedBalance: StateFlow<Boolean>,
    private val selectedDateFlow: StateFlow<LocalDate>,
    private val onDateSelected: (LocalDate) -> Unit,
) : ViewModel() {
    val stateFlow = combine(
        dbAvailableFlow,
        includeCheckedBalance,
    ) { dbState, includeCheckedBalance ->
        return@combine when(dbState) {
            AccountViewModel.DBState.Loading -> {
                State.Loading
            }
            is AccountViewModel.DBState.Error -> {
                State.Error(dbState.error)
            }
            is AccountViewModel.DBState.Loaded -> {
                State.Loaded(
                    includeCheckedBalance = includeCheckedBalance,
                    getDataForMonth = { month ->
                        dbState.db.getDataForMonth(month, includeCheckedBalance)
                    },
                    selectedDateFlow = selectedDateFlow,
                    onDateSelected = onDateSelected,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = State.Loading)

    sealed class State {
        data object Loading : State()
        data class Error(val error: Throwable) : State()
        data class Loaded(
            val includeCheckedBalance: Boolean,
            val getDataForMonth: suspend (YearMonth) -> DataForMonth,
            val selectedDateFlow: StateFlow<LocalDate>,
            val onDateSelected: (LocalDate) -> Unit,
        ) : State()
    }
}