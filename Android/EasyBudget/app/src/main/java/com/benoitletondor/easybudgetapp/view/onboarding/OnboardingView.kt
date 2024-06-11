package com.benoitletondor.easybudgetapp.view.onboarding

import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        onNextButtonPressed = viewModel::onNextButtonPressed,
    )
}

@Composable
private fun OnboardingView(
    eventFlow: Flow<OnboardingViewModel.Event>,
    finishWithResult: (OnboardingResult) -> Unit,
    onBackPressed: (page: Int) -> Unit,
    onNextButtonPressed: (isFinalPage: Boolean) -> Unit,
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
                    pagerState.animateScrollToPage(page = pagerState.currentPage - 1)
                }
                OnboardingViewModel.Event.GoToNextPage -> {
                    pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
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
                    start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                )
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                ) {pageIndex ->
                    when(pageIndex) {
                        0 -> OnboardingPageWelcome(
                            contentPadding = pageContentPadding,
                            onNextPressed = {
                                onNextButtonPressed(false)
                            },
                        )
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
    onNextPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.easy_budget_green))
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_1_title),
                color = Color.White,
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                lineHeight = 46.sp,
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_1_message),
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_1_message2),
                color = Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
            )
        }

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            onClick = onNextPressed,
        ) {
            Text(
                text = stringResource(R.string.onboarding_screen_1_cta),
                fontSize = 20.sp,
            )
        }
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

