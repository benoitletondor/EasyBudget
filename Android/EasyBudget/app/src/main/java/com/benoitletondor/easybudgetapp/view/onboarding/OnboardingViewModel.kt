package com.benoitletondor.easybudgetapp.view.onboarding

import androidx.lifecycle.ViewModel
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(

) : ViewModel() {
    private val mutableEventFlow: MutableLiveFlow<Event> = MutableLiveFlow()
    val eventFlow: Flow<Event> = mutableEventFlow

    sealed class Event {
        data class FinishWithResult(val result: OnboardingResult) : Event()
    }
}