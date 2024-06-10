package com.benoitletondor.easybudgetapp.view.onboarding

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    Scaffold(
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

                val pagerState = rememberPagerState(
                    pageCount = { if (isAndroid33OrMore) 5 else 4 },
                )

                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState,
                ) {pageIndex ->
                    when(pageIndex) {
                        0 -> OnboardingPageWelcome()
                        1 -> OnboardingPageCurrency()
                        2 -> OnboardingPageAccountAmount()
                        3 -> if (isAndroid33OrMore) OnboardingPageEnd() else OnboardingPagePushNotifications()
                        4 -> OnboardingPageEnd()
                    }
                }

                Row(
                    Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.White
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(10.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun OnboardingPageWelcome() {
    // TODO
}

@Composable
private fun OnboardingPageCurrency() {
    // TODO
}

@Composable
private fun OnboardingPageAccountAmount() {
    // TODO
}

@Composable
private fun OnboardingPagePushNotifications() {
    // TODO
}

@Composable
private fun OnboardingPageEnd() {
    // TODO
}

