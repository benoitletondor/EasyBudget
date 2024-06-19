package com.benoitletondor.easybudgetapp.view.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.compose.rememberPermissionStateCompat
import com.benoitletondor.easybudgetapp.helper.AppTheme
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.view.RatingPopup
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyDialog
import com.benoitletondor.easybudgetapp.view.settings.subviews.ErrorView
import com.benoitletondor.easybudgetapp.view.settings.subviews.Settings
import com.benoitletondor.easybudgetapp.view.settings.subviews.ThemePickerDialog
import com.benoitletondor.easybudgetapp.view.settings.subviews.openRedeemCodeDialog
import com.benoitletondor.easybudgetapp.view.settings.subviews.showLowMoneyWarningAmountPickerDialog
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
    navigateToPremium: () -> Unit,
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
        onAdjustLowMoneyWarningAmountChanged = viewModel::onAdjustLowMoneyWarningAmountChanged,
        navigateToPremium = navigateToPremium,
        onThemeSelected = viewModel::onThemeSelected,
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
    onAdjustLowMoneyWarningAmountChanged: (Int) -> Unit,
    navigateToPremium: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val context = LocalContext.current
    val pushPermissionState = rememberPermissionStateCompat()

    LaunchedEffect(pushPermissionState) {
        onPushPermissionResult()
    }

    var showCurrencyPickerDialog by remember { mutableStateOf(false) }
    var showThemePickerDialogWithTheme by remember { mutableStateOf<AppTheme?>(null) }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                SettingsViewModel.Event.OpenBackupSettings -> navigateToBackupSettings()
                SettingsViewModel.Event.ShowCurrencyPicker -> showCurrencyPickerDialog = true
                is SettingsViewModel.Event.ShowLowMoneyWarningAmountPicker -> {
                    context.showLowMoneyWarningAmountPickerDialog(
                        lowMoneyWarningAmount = event.currentLowMoneyWarningAmount,
                        onLowMoneyWarningAmountChanged = onAdjustLowMoneyWarningAmountChanged,
                    )
                }
                SettingsViewModel.Event.AskForNotificationPermission -> {
                    if (pushPermissionState.status.isGranted) {
                        onPushPermissionResult()
                    } else {
                        pushPermissionState.launchPermissionRequest()
                    }
                }
                is SettingsViewModel.Event.OpenBugReport -> {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SENDTO
                    sendIntent.data = Uri.parse("mailto:") // only email apps should handle this
                    sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.bug_report_email)))
                    sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.setting_category_bug_report_send_text, event.localId))
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.setting_category_bug_report_send_subject))

                    if (context.packageManager != null && sendIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(sendIntent)
                    } else {
                        Toast.makeText(context, context.getString(R.string.setting_category_bug_report_send_error), Toast.LENGTH_SHORT).show()
                    }
                }
                SettingsViewModel.Event.OpenRedeemCode -> context.openRedeemCodeDialog()
                SettingsViewModel.Event.OpenSubscribeScreen -> navigateToPremium()
                SettingsViewModel.Event.RedirectToTwitter -> {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://x.com/BenoitLetondor")
                    context.startActivity(i)
                }
                is SettingsViewModel.Event.ShowAppRating -> RatingPopup(context as Activity, event.parameters).show(true)
                SettingsViewModel.Event.ShowAppSharing -> {
                    try {
                        val sendIntent = Intent()
                        sendIntent.action = Intent.ACTION_SEND
                        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.app_invite_message) + "\n" + "https://play.google.com/store/apps/details?id=com.benoitletondor.easybudgetapp")
                        sendIntent.type = "text/plain"
                        context.startActivity(sendIntent)
                    } catch (e: Exception) {
                        Logger.error("An error occurred during sharing app activity start", e)
                    }
                }
                is SettingsViewModel.Event.ShowThemePicker -> showThemePickerDialogWithTheme = event.currentTheme
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

                if (showCurrencyPickerDialog) {
                    SelectCurrencyDialog(
                        onDismissRequest = { showCurrencyPickerDialog = false },
                    )
                }

                val currentThemeForThemePicker = showThemePickerDialogWithTheme
                if (currentThemeForThemePicker != null) {
                    ThemePickerDialog(
                        currentTheme = currentThemeForThemePicker,
                        onThemeSelected = {
                            showThemePickerDialogWithTheme = null
                            onThemeSelected(it)
                        },
                        onDismissRequest = {
                            showThemePickerDialogWithTheme = null
                        },
                    )
                }
            }
        }
    )
}

