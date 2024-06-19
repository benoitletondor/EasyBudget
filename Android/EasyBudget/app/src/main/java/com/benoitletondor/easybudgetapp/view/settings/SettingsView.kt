package com.benoitletondor.easybudgetapp.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.compose.rememberPermissionStateCompat
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.settings.subviews.ErrorView
import com.benoitletondor.easybudgetapp.view.settings.subviews.Settings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class SettingsViewDestination(val redirectToBackupSettings: Boolean)

@Composable
fun SettingsView(
    viewModel: SettingsViewModel,
    navigateUp: () -> Unit,
    navigateToBackupSettings: () -> Unit,
) {
    SettingsView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        navigateToBackupSettings = navigateToBackupSettings,
        onRetryButtonClicked = viewModel::onRetryButtonPressed,
        onCurrencyChangeClicked = viewModel::onCurrencyChangeClicked,
        onAdjustLowMoneyWarningAmountClicked = viewModel::onAdjustLowMoneyWarningAmountClicked,
        onFirstDayOfWeekChanged = viewModel::onFirstDayOfWeekChanged,
        onPremiumButtonClicked = viewModel::onPremiumButtonClicked,
        onProButtonClicked = viewModel::onProButtonClicked,
        onThemeClicked = viewModel::onThemeClicked,
        onShowCheckedBalanceChanged = viewModel::onShowCheckedBalanceChanged,
        onCloudBackupClicked = viewModel::onCloudBackupClicked,
        onDailyReminderNotificationActivatedChanged = viewModel::onDailyReminderNotificationActivatedChanged,
        onMonthlyReportNotificationActivatedChanged = viewModel::onMonthlyReportNotificationActivatedChanged,
        onRateAppClicked = viewModel::onRateAppClicked,
        onShareAppClicked = viewModel::onShareAppClicked,
        onUpdateNotificationActivatedChanged = viewModel::onUpdateNotificationActivatedChanged,
        onBugReportClicked = viewModel::onBugReportClicked,
        onAppClicked = viewModel::onAppClicked,
        onSubscribeButtonClicked = viewModel::onSubscribeButtonClicked,
        onRedeemCodeButtonClicked =  viewModel::onRedeemCodeButtonClicked,
        onPushPermissionResult = viewModel::onPushPermissionResult,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SettingsView(
    stateFlow: StateFlow<SettingsViewModel.State>,
    eventFlow: Flow<SettingsViewModel.Event>,
    navigateUp: () -> Unit,
    navigateToBackupSettings: () -> Unit,
    onRetryButtonClicked: () -> Unit,
    onCurrencyChangeClicked: () -> Unit,
    onAdjustLowMoneyWarningAmountClicked: () -> Unit,
    onFirstDayOfWeekChanged: (DayOfWeek) -> Unit,
    onPremiumButtonClicked: () -> Unit,
    onProButtonClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    onShowCheckedBalanceChanged: (Boolean) -> Unit,
    onCloudBackupClicked: () -> Unit,
    onDailyReminderNotificationActivatedChanged: (Boolean) -> Unit,
    onMonthlyReportNotificationActivatedChanged: (Boolean) -> Unit,
    onRateAppClicked: () -> Unit,
    onShareAppClicked: () -> Unit,
    onUpdateNotificationActivatedChanged: (Boolean) -> Unit,
    onBugReportClicked: () -> Unit,
    onAppClicked: () -> Unit,
    onSubscribeButtonClicked: () -> Unit,
    onRedeemCodeButtonClicked: () -> Unit,
    onPushPermissionResult: () -> Unit,
) {
    val pushPermissionState = rememberPermissionStateCompat()

    LaunchedEffect(pushPermissionState) {
        onPushPermissionResult()
    }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                SettingsViewModel.Event.OpenBackupSettings -> navigateToBackupSettings()
                SettingsViewModel.Event.ShowCurrencyPicker -> TODO()
                SettingsViewModel.Event.ShowLowMoneyWarningAmountPicker -> TODO()
                SettingsViewModel.Event.AskForNotificationPermission -> {
                    if (pushPermissionState.status.isGranted) {
                        onPushPermissionResult()
                    } else {
                        pushPermissionState.launchPermissionRequest()
                    }
                }
                SettingsViewModel.Event.OpenBugReport -> TODO()
                SettingsViewModel.Event.OpenRedeemCode -> TODO()
                SettingsViewModel.Event.OpenSubscribeScreen -> TODO()
                SettingsViewModel.Event.RedirectToTwitter -> TODO()
                SettingsViewModel.Event.ShowAppRating -> TODO()
                SettingsViewModel.Event.ShowAppSharing -> TODO()
                SettingsViewModel.Event.ShowThemePicker -> TODO()
            }
        }
    }

    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_settings),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Box(
                modifier = Modifier.padding(contentPadding),
            ) {
                val state by stateFlow.collectAsState()

                when(val currentState = state) {
                    is SettingsViewModel.State.Error -> ErrorView(
                        error = currentState.error,
                        onRetryButtonClicked = onRetryButtonClicked,
                    )
                    is SettingsViewModel.State.Loaded -> Settings(
                        state = currentState,
                        onCurrencyChangeClicked = onCurrencyChangeClicked,
                        onAdjustLowMoneyWarningAmountClicked = onAdjustLowMoneyWarningAmountClicked,
                        onFirstDayOfWeekChanged = onFirstDayOfWeekChanged,
                        onPremiumButtonClicked = onPremiumButtonClicked,
                        onProButtonClicked = onProButtonClicked,
                        onThemeClicked = onThemeClicked,
                        onShowCheckedBalanceChanged = onShowCheckedBalanceChanged,
                        onCloudBackupClicked = onCloudBackupClicked,
                        onDailyReminderNotificationActivatedChanged = onDailyReminderNotificationActivatedChanged,
                        onMonthlyReportNotificationActivatedChanged = onMonthlyReportNotificationActivatedChanged,
                        onRateAppClicked = onRateAppClicked,
                        onShareAppClicked = onShareAppClicked,
                        onUpdateNotificationActivatedChanged= onUpdateNotificationActivatedChanged,
                        onBugReportClicked = onBugReportClicked,
                        onAppClicked = onAppClicked,
                        onSubscribeButtonClicked = onSubscribeButtonClicked,
                        onRedeemCodeButtonClicked = onRedeemCodeButtonClicked,
                    )
                    SettingsViewModel.State.Loading -> LoadingView()
                }
            }
        }
    )
}

