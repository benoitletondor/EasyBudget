package com.benoitletondor.easybudgetapp.view.onboarding

import android.Manifest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

private fun pageIndexToOnboardingPage(index: Int): OnboardingViewModel.OnboardingPage {
    val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

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
    val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

    val pagerState = rememberPagerState(
        pageCount = { if (isAndroid33OrMore) 5 else 4 },
    )

    val pushPermissionState = if (isAndroid33OrMore) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        remember {
            object : PermissionState {
                override val permission: String = "android.permission.POST_NOTIFICATIONS"
                override val status: PermissionStatus = PermissionStatus.Granted
                override fun launchPermissionRequest() {
                    /* No-op */
                }
            }
        }
    }

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
                fontSize = 16.sp,
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
    userCurrencyFlow: StateFlow<Currency>,
    onNextPressed: () -> Unit,
) {
    val selectedCurrency by userCurrencyFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_2_title),
                color = Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_2_message),
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )

            SelectCurrencyView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .padding(horizontal = 20.dp, vertical = 30.dp)
                    .background(color = Color.White),
            )
        }

        Button(
            onClick = onNextPressed,
        ) {
            Text(
                text = stringResource(R.string.onboarding_screen_2_cta, selectedCurrency.symbol),
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
private fun OnboardingPageAccountAmount(
    contentPadding: PaddingValues,
    pagerState: PagerState,
    userCurrencyFlow: StateFlow<Currency>,
    userMoneyAmountFlow: StateFlow<Double>,
    onNextPressed: () -> Unit,
    onAmountChange: (String) -> Unit,
) {
    val currency by userCurrencyFlow.collectAsState()
    val currentAmount by userMoneyAmountFlow.collectAsState()

    var currentTextFieldValue by remember { mutableStateOf(TextFieldValue(
        text = "",
        selection = TextRange(index = 0),
    )) }

    LaunchedEffect("initAmount") {
        val amountFromDB = userMoneyAmountFlow.value
        val formattedAmount = formatAmountValue(amountFromDB)
        if (amountFromDB != 0.0 && formattedAmount != currentTextFieldValue.text) {
            currentTextFieldValue = TextFieldValue(
                text = formattedAmount,
                selection = TextRange(index = formattedAmount.length),
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val shouldRequestFocus by remember {
        derivedStateOf {
            pagerState.currentPageOffsetFraction == 0f && pageIndexToOnboardingPage(pagerState.currentPage) == OnboardingViewModel.OnboardingPage.INITIAL_AMOUNT
        }
    }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusRequester.freeFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
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
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_3_title),
                color = Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_3_message),
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ){
                TextField(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    value = currentTextFieldValue,
                    onValueChange = { newValue ->
                        if (newValue.text.all { "-0123456789.,".contains(it) }) {
                            val newText = if (newValue.text.count() > 1 && newValue.text.endsWith("-")) {
                                if (newValue.text.startsWith("-")) {
                                    newValue.text.substring(1, newValue.text.length - 1)
                                } else {
                                    "-${newValue.text.substring(0, newValue.text.length - 1)}"
                                }
                            } else {
                                newValue.text
                            }

                            currentTextFieldValue = TextFieldValue(
                                text = newText,
                                selection = newValue.selection,
                            )
                            onAmountChange(newText)
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        autoCorrectEnabled = false,
                    ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = currency.symbol,
                    color = Color.White,
                    fontSize = 30.sp,
                )
            }

        }

        Button(
            onClick = onNextPressed,
        ) {
            Text(
                text = stringResource(R.string.onboarding_screen_3_cta, CurrencyHelper.getFormattedCurrencyString(currency, currentAmount)),
                fontSize = 20.sp,
            )
        }
    }
}

private fun formatAmountValue(amount: Double): String = if (amount == 0.0) "0" else amount.toString()

@Composable
private fun OnboardingPagePushNotifications(
    contentPadding: PaddingValues,
    onAcceptNotificationsPressed: () -> Unit,
    onDenyNotificationsPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.secondary))
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
                painter = painterResource(R.drawable.ic_baseline_notification_important_24),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_push_permission_title),
                color = Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_screen_push_permission_message),
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                modifier = Modifier.weight(0.5f),
                onClick = onDenyNotificationsPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.accent_ripple),
                    contentColor = colorResource(R.color.easy_budget_green_dark),
                ),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_screen_push_permission_not_now_cta),
                    fontSize = 20.sp,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                modifier = Modifier.weight(0.5f),
                onClick = onAcceptNotificationsPressed,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_screen_push_permission_accept_cta),
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageEnd(
    contentPadding: PaddingValues,
) {

}

