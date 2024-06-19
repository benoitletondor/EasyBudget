package com.benoitletondor.easybudgetapp.view.settings.subviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.AppTheme.*
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.view.settings.SettingsViewModel
import java.time.DayOfWeek

@Composable
fun Settings(
    state: SettingsViewModel.State.Loaded,
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "generalCategory") {
            SettingsCategoryTitle(title = stringResource(R.string.setting_category_general_title))
        }

        item(key = "currencyChangeButton") {
            SettingsButton(
                title = stringResource(R.string.setting_category_currency_change_button_title, state.userCurrency.symbol),
                subtitle = stringResource(R.string.setting_category_currency_change_button_message),
                onClick = onCurrencyChangeClicked,
            )
        }

        item(key = "lowMoneyWarningAmountButton") {
            SettingsButton(
                title = stringResource(R.string.setting_category_limit_set_button_title, CurrencyHelper.getFormattedCurrencyString(state.userCurrency, state.lowMoneyWarningAmount.toDouble())),
                subtitle = stringResource(R.string.setting_category_limit_set_button_message),
                onClick = onAdjustLowMoneyWarningAmountClicked,
            )
        }

        item(key = "startDayOfWeekSwitch") {
            SettingsSwitch(
                title = stringResource(R.string.setting_category_start_day_of_week_title),
                subtitle = stringResource(if (state.firstDayOfWeek == DayOfWeek.SUNDAY) R.string.setting_category_start_day_of_week_sunday else R.string.setting_category_start_day_of_week_monday),
                checked = state.firstDayOfWeek == DayOfWeek.SUNDAY,
                onCheckedChanged = { checked ->
                    if (checked) {
                        onFirstDayOfWeekChanged(DayOfWeek.SUNDAY)
                    } else {
                        onFirstDayOfWeekChanged(DayOfWeek.MONDAY)
                    }
                }
            )
        }

        when(val subscriptionStatus = state.subscriptionStatus) {
            SettingsViewModel.SubscriptionStatus.Error -> {
                item(key = "notSubscribedCategory") {
                    SettingsCategoryTitle(title = stringResource(R.string.setting_category_not_premium_title))
                }

                item(key = "subscriptionError") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.premium_screen_error_loading_message),
                        color = colorResource(R.color.primary_text),
                        fontSize = 16.sp,
                    )
                }
            }
            SettingsViewModel.SubscriptionStatus.Loading -> {
                item(key = "notSubscribedCategory") {
                    SettingsCategoryTitle(title = stringResource(R.string.setting_category_not_premium_title))
                }

                item(key = "subscriptionLoading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            SettingsViewModel.SubscriptionStatus.NotSubscribed -> {
                item(key = "notSubscribedCategory") {
                    SettingsCategoryTitle(title = stringResource(R.string.setting_category_not_premium_title))
                }

                item(key = "subscribeButton") {
                    SettingsButton(
                        title = stringResource(R.string.setting_category_not_premium_status_title),
                        subtitle = stringResource(R.string.setting_category_not_premium_status_message),
                        onClick = onSubscribeButtonClicked,
                    )
                }

                item(key = "redeemCode") {
                    SettingsButton(
                        title = stringResource(R.string.setting_category_premium_redeem_title),
                        subtitle = stringResource(R.string.setting_category_premium_redeem_message),
                        onClick = onRedeemCodeButtonClicked,
                    )
                }
            }
            is SettingsViewModel.SubscriptionStatus.Subscribed -> {
                item(key = "subscriptionCategory") {
                    SettingsCategoryTitle(title = stringResource(R.string.setting_category_premium_title))
                }

                when(subscriptionStatus) {
                    is SettingsViewModel.SubscriptionStatus.PremiumSubscribed -> {
                        item(key = "premiumSubscribed") {
                            SettingsButton(
                                title = stringResource(R.string.setting_category_premium_status_title),
                                subtitle = stringResource(R.string.setting_category_premium_status_message),
                                onClick = onPremiumButtonClicked,
                            )
                        }
                    }
                    is SettingsViewModel.SubscriptionStatus.ProSubscribed -> {
                        item(key = "proSubscribed") {
                            SettingsButton(
                                title = stringResource(R.string.setting_category_pro_status_title),
                                subtitle = stringResource(R.string.setting_category_pro_status_message),
                                onClick = onProButtonClicked,
                            )
                        }
                    }
                }

                item(key = "theme") {
                    SettingsButton(
                        title = stringResource(R.string.setting_category_theme_title),
                        subtitle = stringResource(when(subscriptionStatus.theme) {
                            LIGHT -> R.string.setting_theme_light
                            DARK -> R.string.setting_theme_dark
                            PLATFORM_DEFAULT -> R.string.setting_theme_platform
                        }),
                        onClick = onThemeClicked,
                    )
                }

                item(key = "checkedBalance") {
                    SettingsSwitch(
                        title = stringResource(R.string.setting_category_show_checked_balance_title),
                        subtitle = stringResource(R.string.setting_category_show_checked_balance_message),
                        checked = subscriptionStatus.showCheckedBalance,
                        onCheckedChanged = onShowCheckedBalanceChanged,
                    )
                }

                item(key = "cloudBackup") {
                    SettingsButton(
                        title = stringResource(R.string.backup_settings_activity_title),
                        subtitle = stringResource(if (subscriptionStatus.cloudBackupEnabled) R.string.backup_settings_backups_activated else R.string.backup_settings_backups_deactivated),
                        onClick = onCloudBackupClicked
                    )
                }

                item(key = "dailyReminderNotification") {
                    SettingsCheckbox(
                        title = stringResource(R.string.setting_category_notifications_daily_title),
                        subtitle = stringResource(R.string.setting_category_notifications_daily_message),
                        checked = subscriptionStatus.dailyReminderActivated,
                        onCheckedChanged = onDailyReminderNotificationActivatedChanged,
                    )
                }

                item(key = "monthlyReportNotification") {
                    SettingsCheckbox(
                        title = stringResource(R.string.setting_category_notifications_monthly_title),
                        subtitle = stringResource(R.string.setting_category_notifications_monthly_message),
                        checked = subscriptionStatus.monthlyReportNotificationActivated,
                        onCheckedChanged = onMonthlyReportNotificationActivatedChanged,
                    )
                }
            }
        }

        item(key = "appCategory") {
            SettingsCategoryTitle(title = stringResource(R.string.setting_category_app_title))
        }

        item(key = "rating") {
            SettingsButton(
                title = stringResource(R.string.setting_category_rate_button_title),
                subtitle = stringResource(R.string.setting_category_rate_button_message),
                onClick = onRateAppClicked,
            )
        }

        item(key = "share") {
            SettingsButton(
                title = stringResource(R.string.setting_category_share_app_title),
                subtitle = stringResource(R.string.setting_category_share_app_message),
                onClick = onShareAppClicked,
            )
        }

        item(key = "updatesNotifications") {
            SettingsCheckbox(
                title = stringResource(R.string.setting_category_notifications_update_title),
                subtitle = stringResource(R.string.setting_category_notifications_update_message),
                checked = state.userAllowingUpdatePushes,
                onCheckedChanged = onUpdateNotificationActivatedChanged,
            )
        }

        item(key = "bugReport") {
            SettingsButton(
                title = stringResource(R.string.setting_category_bug_report_send_button_title),
                onClick = onBugReportClicked,
            )
        }

        item(key = "app") {
            SettingsButton(
                title = stringResource(R.string.setting_category_app_version_title, state.appVersion),
                subtitle = stringResource(R.string.setting_category_app_version_message),
                onClick = onAppClicked,
            )
        }
    }
}