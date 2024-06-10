package com.benoitletondor.easybudgetapp.view.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.helper.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
object OnboardingDestination

@Serializable
data class OnboardingResult(val onboardingCompleted: Boolean)

@Composable
fun OnboardingView(
    viewModel: OnboardingViewModel = hiltViewModel(),
    finishWithResult: (OnboardingResult) -> Unit,
) {
    OnboardingView(
        eventFlow = viewModel.eventFlow,
        finishWithResult = finishWithResult,
    )
}

@Composable
private fun OnboardingView(
    eventFlow: Flow<OnboardingViewModel.Event>,
    finishWithResult: (OnboardingResult) -> Unit,
) {
    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is OnboardingViewModel.Event.FinishWithResult -> finishWithResult(event.result)
            }
        }
    }
}