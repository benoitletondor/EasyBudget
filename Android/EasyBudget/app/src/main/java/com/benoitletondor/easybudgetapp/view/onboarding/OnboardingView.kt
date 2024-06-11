package com.benoitletondor.easybudgetapp.view.onboarding

import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.launchCollect
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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
        finishWithResult = finishWithResult,
        onBackPressed = viewModel::onBackPressed,
    )
}

@Composable
private fun OnboardingView(
    eventFlow: Flow<OnboardingViewModel.Event>,
    finishWithResult: (OnboardingResult) -> Unit,
    onBackPressed: (page: Int) -> Unit,
) {
    val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

    val pagerState = rememberPagerState(
        pageCount = { if (isAndroid33OrMore) 5 else 4 },
    )

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                is OnboardingViewModel.Event.FinishWithResult -> finishWithResult(event.result)
                OnboardingViewModel.Event.GoToPreviousPage -> {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        }
    }

    BackHandler {
        onBackPressed(pagerState.currentPage)
    }

    Scaffold(
        content = { contentPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val pageContentPadding = remember {
                PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                ) {pageIndex ->
                    when(pageIndex) {
                        0 -> OnboardingPageWelcome(contentPadding = pageContentPadding)
                        1 -> OnboardingPageCurrency(contentPadding = pageContentPadding)
                        2 -> OnboardingPageAccountAmount(contentPadding = pageContentPadding)
                        3 -> if (isAndroid33OrMore) OnboardingPagePushNotifications(contentPadding =pageContentPadding) else OnboardingPageEnd(contentPadding = pageContentPadding)
                        4 -> OnboardingPageEnd(contentPadding = pageContentPadding)
                    }
                }

                Row(
                    Modifier
                        .padding(contentPadding)
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding()
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

@Composable
private fun OnboardingPageWelcome(
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.easy_budget_green))
            .padding(contentPadding),
    ) {

    }
}

@Composable
private fun OnboardingPageCurrency(
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
            .padding(contentPadding),
    ) {

    }
}

@Composable
private fun OnboardingPageAccountAmount(
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
            .padding(contentPadding),
    ) {

    }
}

@Composable
private fun OnboardingPagePushNotifications(
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
            .padding(contentPadding),
    ) {

    }
}

@Composable
private fun OnboardingPageEnd(
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.easy_budget_green))
            .padding(contentPadding),
    ) {

    }
}

