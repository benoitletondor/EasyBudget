/*
 *   Copyright 2024 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.easybudgetapp.view.onboarding

import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.compose.rememberPermissionStateCompat
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.onboarding.subviews.OnboardingPageAccountAmount
import com.benoitletondor.easybudgetapp.view.onboarding.subviews.OnboardingPageCurrency
import com.benoitletondor.easybudgetapp.view.onboarding.subviews.OnboardingPageEnd
import com.benoitletondor.easybudgetapp.view.onboarding.subviews.OnboardingPagePushNotifications
import com.benoitletondor.easybudgetapp.view.onboarding.subviews.OnboardingPageWelcome
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.lang.IllegalStateException
import java.util.Currency

@Serializable
object OnboardingDestination

@Parcelize
data class OnboardingResult(val onboardingCompleted: Boolean) : Parcelable

@Composable
fun OnboardingView(
    viewModel: OnboardingViewModel = hiltViewModel(),
    finishWithResult: (OnboardingResult) -> Unit,
) {
    OnboardingView(
        eventFlow = viewModel.eventFlow,
        userCurrencyFlow = viewModel.userCurrencyFlow,
        userMoneyAmountFlow = viewModel.userMoneyAmountFlow,
        finishWithResult = finishWithResult,
        onBackPressed = viewModel::onBackPressed,
        onNextButtonPressed = viewModel::onNextButtonPressed,
        onAmountChange = viewModel::onAmountChange,
        onAcceptNotificationsPressed = viewModel::onAcceptNotificationsPressed,
        onDenyNotificationsPressed = viewModel::onDenyNotificationsPressed,
        onPushNotificationsResponse = viewModel::onPushNotificationsResponse,
    )
}

private val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

fun pageIndexToOnboardingPage(index: Int): OnboardingViewModel.OnboardingPage {
    return when(index) {
        0 -> OnboardingViewModel.OnboardingPage.WELCOME
        1 -> OnboardingViewModel.OnboardingPage.CURRENCY
        2 -> OnboardingViewModel.OnboardingPage.INITIAL_AMOUNT
        3 -> if (isAndroid33OrMore) OnboardingViewModel.OnboardingPage.PUSH_NOTIFICATIONS else OnboardingViewModel.OnboardingPage.END
        4 -> OnboardingViewModel.OnboardingPage.END
        else -> throw IllegalStateException("Unknown onboarding page index: $index")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OnboardingView(
    eventFlow: Flow<OnboardingViewModel.Event>,
    userCurrencyFlow: StateFlow<Currency>,
    userMoneyAmountFlow: StateFlow<Double>,
    finishWithResult: (OnboardingResult) -> Unit,
    onBackPressed: (page: OnboardingViewModel.OnboardingPage) -> Unit,
    onNextButtonPressed: (page: OnboardingViewModel.OnboardingPage) -> Unit,
    onAmountChange: (String) -> Unit,
    onAcceptNotificationsPressed: () -> Unit,
    onDenyNotificationsPressed: () -> Unit,
    onPushNotificationsResponse: (OnboardingViewModel.OnboardingPage) -> Unit,
) {
    val pagerState = rememberPagerState(
        pageCount = { if (isAndroid33OrMore) 5 else 4 },
    )

    val pushPermissionState = rememberPermissionStateCompat()

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is OnboardingViewModel.Event.FinishWithResult -> finishWithResult(event.result)
                OnboardingViewModel.Event.GoToPreviousPage -> {
                    pagerState.animateScrollToPage(page = pagerState.currentPage - 1)
                }
                OnboardingViewModel.Event.GoToNextPage -> {
                    pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
                }
                OnboardingViewModel.Event.RequestPushPermission -> {
                    if (pushPermissionState.status.isGranted) {
                        onPushNotificationsResponse(pageIndexToOnboardingPage(pagerState.currentPage))
                    } else {
                        pushPermissionState.launchPermissionRequest()
                    }
                }
            }
        }
    }

    LaunchedEffect(pushPermissionState) {
        onPushNotificationsResponse(pageIndexToOnboardingPage(pagerState.currentPage))
    }

    BackHandler {
        onBackPressed(pageIndexToOnboardingPage(pagerState.currentPage))
    }

    Scaffold(
        content = { contentPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val pageContentPadding = remember {
                PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                ) {pageIndex ->
                    when(val page = pageIndexToOnboardingPage(pageIndex)) {
                        OnboardingViewModel.OnboardingPage.WELCOME -> OnboardingPageWelcome(
                            contentPadding = pageContentPadding,
                            onNextPressed = { onNextButtonPressed(page) },
                        )
                        OnboardingViewModel.OnboardingPage.CURRENCY ->  OnboardingPageCurrency(
                            contentPadding = pageContentPadding,
                            userCurrencyFlow = userCurrencyFlow,
                            onNextPressed = { onNextButtonPressed(page) },
                        )
                        OnboardingViewModel.OnboardingPage.INITIAL_AMOUNT -> OnboardingPageAccountAmount(
                            contentPadding = pageContentPadding,
                            pagerState = pagerState,
                            userCurrencyFlow = userCurrencyFlow,
                            userMoneyAmountFlow = userMoneyAmountFlow,
                            onNextPressed = { onNextButtonPressed(page) },
                            onAmountChange = onAmountChange,
                        )
                        OnboardingViewModel.OnboardingPage.PUSH_NOTIFICATIONS -> OnboardingPagePushNotifications(
                            contentPadding = pageContentPadding,
                            onAcceptNotificationsPressed = onAcceptNotificationsPressed,
                            onDenyNotificationsPressed = onDenyNotificationsPressed,
                        )
                        OnboardingViewModel.OnboardingPage.END -> OnboardingPageEnd(
                            contentPadding = pageContentPadding,
                            onNextPressed = { onNextButtonPressed(page) },
                        )
                    }
                }

                Row(
                    Modifier
                        .padding(contentPadding)
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = .6f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 8.dp else 5.dp)
                        )
                    }
                }
            }
        }
    )
}
