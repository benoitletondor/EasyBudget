package com.benoitletondor.easybudgetapp.view.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(

) : ViewModel() {
    private val mutableEventFlow: MutableLiveFlow<Event> = MutableLiveFlow()
    val eventFlow: Flow<Event> = mutableEventFlow

    fun onBackPressed(page: Int) {
        viewModelScope.launch {
            if (page == 0) {
                mutableEventFlow.emit(Event.FinishWithResult(OnboardingResult(onboardingCompleted = false)))
            } else {
                mutableEventFlow.emit(Event.GoToPreviousPage)
            }
        }
    }

    fun onNextButtonPressed(isFinalPage: Boolean) {
        viewModelScope.launch {
            if (isFinalPage) {
                mutableEventFlow.emit(Event.FinishWithResult(OnboardingResult(onboardingCompleted = true)))
            } else {
                mutableEventFlow.emit(Event.GoToNextPage)
            }
        }
    }

    sealed class Event {
        data class FinishWithResult(val result: OnboardingResult) : Event()
        data object GoToPreviousPage : Event()
        data object GoToNextPage : Event()
    }
}