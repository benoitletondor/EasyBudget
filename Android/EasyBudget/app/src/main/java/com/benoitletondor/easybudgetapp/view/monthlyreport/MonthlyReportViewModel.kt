package com.benoitletondor.easybudgetapp.view.monthlyreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.injection.CurrentDBProvider
import com.benoitletondor.easybudgetapp.view.report.base.MonthlyReportBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    iab: Iab,
    private val currentDBProvider: CurrentDBProvider,
) : ViewModel() {

    val shouldShowExportToCsvButtonFlow: StateFlow<Boolean> = iab.iabStatusFlow
        .map { it == PremiumCheckStatus.PRO_SUBSCRIBED }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
}