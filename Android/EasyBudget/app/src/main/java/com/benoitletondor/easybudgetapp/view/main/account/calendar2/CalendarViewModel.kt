package com.benoitletondor.easybudgetapp.view.main.account.calendar2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.view.main.account.AccountViewModel
import com.kizitonwose.calendar.core.yearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val dbAvailableFlow: StateFlow<AccountViewModel.DBState>,
    private val selectedDateFlow: StateFlow<LocalDate>,
    private val onDateSelected: (LocalDate) -> Unit,
) : ViewModel() {
    val displayedMonthFlow = selectedDateFlow
        .map { it.yearMonth }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = selectedDateFlow.value.yearMonth)
}