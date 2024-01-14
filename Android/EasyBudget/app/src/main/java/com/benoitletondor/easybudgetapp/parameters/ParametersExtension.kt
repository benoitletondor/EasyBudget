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

package com.benoitletondor.easybudgetapp.parameters

import com.benoitletondor.easybudgetapp.helper.AppTheme
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Date

private const val DEFAULT_LOW_MONEY_WARNING_AMOUNT = 100
/**
 * Timestamp of the base balance set-up (long)
 */
private const val INIT_TIMESTAMP_PARAMETERS_KEY = "init_date"
/**
 * Local identifier of the device (generated on first launch) (string)
 */
private const val LOCAL_ID_PARAMETERS_KEY = "local_id"
/**
 * Number of time different day has been open (int)
 */
private const val NUMBER_OF_DAILY_OPEN_PARAMETERS_KEY = "number_of_daily_open"
/**
 * Timestamp that indicates the last time user was presented the rating popup (long)
 */
private const val RATING_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY = "rating_popup_last_auto_show"
/**
 * Has the user complete the premium popup = should we show it again or not (bool)
 */
private const val PREMIUM_POPUP_COMPLETE_PARAMETERS_KEY = "premium_popup_complete"
/**
 * Timestamp that indicates the last time user was presented the become premium popup (long)
 */
private const val PREMIUM_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY = "premium_popup_last_auto_show"
/**
 * App version stored to detect updates (int)
 */
private const val APP_VERSION_PARAMETERS_KEY = "appversion"
/**
 * Number of time the app has been opened (int)
 */
private const val NUMBER_OF_OPEN_PARAMETERS_KEY = "number_of_open"
/**
 * Timestamp of the last open (long)
 */
private const val LAST_OPEN_DATE_PARAMETERS_KEY = "last_open_date"
/**
 * Warning limit for low money on account (int)
 */
private const val LOW_MONEY_WARNING_AMOUNT_PARAMETERS_KEY = "low_money_warning_amount"
/**
 * First day of week to show (int)
 */
private const val FIRST_DAY_OF_WEEK_PARAMETERS_KEY = "first_day_of_week"
/**
 * The user wants to receive notifications for updates (bool)
 */
private const val USER_ALLOW_UPDATE_PUSH_PARAMETERS_KEY = "user_allow_update_push"
/**
 * The user wants to receive a daily reminder notification (bool)
 */
private const val USER_ALLOW_DAILY_PUSH_PARAMETERS_KEY = "user_allow_daily_push"
/**
 * The user wants to receive a monthly reminder notification when report is available (bool)
 */
private const val USER_ALLOW_MONTHLY_PUSH_PARAMETERS_KEY = "user_allow_monthly_push"
/**
 * Indicate if the rating has been completed by the user (finished or not ask me again) (bool)
 */
private const val RATING_COMPLETED_PARAMETERS_KEY = "rating_completed"
/**
 * Has the user saw the monthly report hint (bool)
 */
private const val USER_SAW_MONTHLY_REPORT_HINT_PARAMETERS_KEY = "user_saw_monthly_report_hint"
/**
 * AppTheme
 */
private const val APP_THEME_PARAMETERS_KEY = "app_theme"
/**
 * Backup enabled
 */
private const val BACKUP_ENABLED_PARAMETERS_KEY = "backup_enabled"
/**
 * Last successful backup timestamp
 */
private const val LAST_BACKUP_TIMESTAMP = "last_backup_ts"
/**
 * Should reset init date at next app launch after backup restore
 */
private const val SHOULD_RESET_INIT_DATE = "should_reset_init_date"
/**
 * Should show the checked balance in main screen
 */
private const val SHOULD_SHOW_CHECKED_BALANCE = "should_show_checked_balance"
/**
 * Id of the last selected online account
 */
private const val SELECTED_ACCOUNT_ID_KEY = "selectedAccountId"

fun Parameters.getInitDate(): LocalDate? {
    val timestamp = getLong(INIT_TIMESTAMP_PARAMETERS_KEY, 0L)
    if (timestamp <= 0L) {
        return null
    }

    return localDateFromTimestamp(timestamp)
}

fun Parameters.setInitDate(date: Date) {
    putLong(INIT_TIMESTAMP_PARAMETERS_KEY, date.time)
}

fun Parameters.getLocalId(): String? {
    return getString(LOCAL_ID_PARAMETERS_KEY)
}

fun Parameters.setLocalId(localId: String) {
    putString(LOCAL_ID_PARAMETERS_KEY, localId)
}

fun Parameters.getNumberOfDailyOpen(): Int {
    return getInt(NUMBER_OF_DAILY_OPEN_PARAMETERS_KEY, 0)
}

fun Parameters.setNumberOfDailyOpen(numberOfDailyOpen: Int) {
    putInt(NUMBER_OF_DAILY_OPEN_PARAMETERS_KEY, numberOfDailyOpen)
}

fun Parameters.getRatingPopupLastAutoShowTimestamp(): Long {
    return getLong(RATING_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY, 0)
}

fun Parameters.setRatingPopupLastAutoShowTimestamp(timestamp: Long) {
    putLong(RATING_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY, timestamp)
}

fun Parameters.hasPremiumPopupBeenShow(): Boolean {
    return getBoolean(PREMIUM_POPUP_COMPLETE_PARAMETERS_KEY, false)
}

fun Parameters.setPremiumPopupShown() {
    putBoolean(PREMIUM_POPUP_COMPLETE_PARAMETERS_KEY, true)
}

fun Parameters.getPremiumPopupLastAutoShowTimestamp(): Long {
    return getLong(PREMIUM_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY, 0)
}

fun Parameters.setPremiumPopupLastAutoShowTimestamp(timestamp: Long) {
    putLong(PREMIUM_POPUP_LAST_AUTO_SHOW_PARAMETERS_KEY, timestamp)
}

fun Parameters.getCurrentAppVersion(): Int {
    return getInt(APP_VERSION_PARAMETERS_KEY, 0)
}

fun Parameters.setCurrentAppVersion(appVersion: Int) {
    putInt(APP_VERSION_PARAMETERS_KEY, appVersion)
}

fun Parameters.getNumberOfOpen(): Int {
    return getInt(NUMBER_OF_OPEN_PARAMETERS_KEY, 0)
}

fun Parameters.setNumberOfOpen(numberOfOpen: Int) {
    putInt(NUMBER_OF_OPEN_PARAMETERS_KEY, numberOfOpen)
}

fun Parameters.getLastOpenTimestamp(): Long {
    return getLong(LAST_OPEN_DATE_PARAMETERS_KEY, 0)
}

fun Parameters.setLastOpenTimestamp(timestamp: Long) {
    putLong(LAST_OPEN_DATE_PARAMETERS_KEY, timestamp)
}

private lateinit var lowMoneyWarningAmountFlow: MutableStateFlow<Int>

fun Parameters.watchLowMoneyWarningAmount(): StateFlow<Int> {
    if (!::lowMoneyWarningAmountFlow.isInitialized) {
        lowMoneyWarningAmountFlow = MutableStateFlow(getLowMoneyWarningAmount())
    }

    return lowMoneyWarningAmountFlow
}

fun Parameters.getLowMoneyWarningAmount(): Int {
    return getInt(LOW_MONEY_WARNING_AMOUNT_PARAMETERS_KEY, DEFAULT_LOW_MONEY_WARNING_AMOUNT)
}

fun Parameters.setLowMoneyWarningAmount(amount: Int) {
    if (!::lowMoneyWarningAmountFlow.isInitialized) {
        lowMoneyWarningAmountFlow = MutableStateFlow(amount)
    }

    lowMoneyWarningAmountFlow.value = amount
    putInt(LOW_MONEY_WARNING_AMOUNT_PARAMETERS_KEY, amount)
}

private lateinit var firstDayOfWeekFlow: MutableStateFlow<DayOfWeek>

fun Parameters.watchFirstDayOfWeek(): StateFlow<DayOfWeek> {
    if (!::firstDayOfWeekFlow.isInitialized) {
        firstDayOfWeekFlow = MutableStateFlow(getFirstDayOfWeek())
    }

    return firstDayOfWeekFlow
}

/**
 * Get the first day of the week to display to the user
 */
fun Parameters.getFirstDayOfWeek(): DayOfWeek {
    val currentValue = getInt(FIRST_DAY_OF_WEEK_PARAMETERS_KEY, -1)
    return currentValue.toDayOfWeek()
}

/**
 * Set the first day of week to display to the user
 */
fun Parameters.setFirstDayOfWeek(dayOfWeek: DayOfWeek) {
    if (!::firstDayOfWeekFlow.isInitialized) {
        firstDayOfWeekFlow = MutableStateFlow(dayOfWeek)
    }

    firstDayOfWeekFlow.value = dayOfWeek
    putInt(FIRST_DAY_OF_WEEK_PARAMETERS_KEY, dayOfWeek.toInt())
}

private fun DayOfWeek.toInt(): Int {
    return when(this) {
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
        DayOfWeek.SATURDAY -> 7
        DayOfWeek.SUNDAY -> 1
    }
}

private fun Int.toDayOfWeek(): DayOfWeek {
    return when(this){
        1 -> DayOfWeek.SUNDAY
        3 -> DayOfWeek.TUESDAY
        4 -> DayOfWeek.WEDNESDAY
        5 -> DayOfWeek.THURSDAY
        6 -> DayOfWeek.FRIDAY
        7 -> DayOfWeek.SATURDAY
        else -> DayOfWeek.MONDAY
    }
}

/**
 * The user wants or not to receive notification about updates
 *
 * @return true if we can display update notifications, false otherwise
 */
fun Parameters.isUserAllowingUpdatePushes(): Boolean {
    return getBoolean(USER_ALLOW_UPDATE_PUSH_PARAMETERS_KEY, true)
}

/**
 * Set the user choice about update notifications
 *
 * @param value if the user wants or not to receive notifications about updates
 */
fun Parameters.setUserAllowUpdatePushes(value: Boolean) {
    putBoolean(USER_ALLOW_UPDATE_PUSH_PARAMETERS_KEY, value)
}

/**
 * The user wants or not to receive a daily reminder notification
 *
 * @return true if we can display daily notifications, false otherwise
 */
fun Parameters.isUserAllowingDailyReminderPushes(): Boolean {
    return getBoolean(USER_ALLOW_DAILY_PUSH_PARAMETERS_KEY, true)
}

/**
 * Set the user choice about daily reminder notifications
 *
 * @param value if the user wants or not to receive daily notifications
 */
fun Parameters.setUserAllowDailyReminderPushes(value: Boolean) {
    putBoolean(USER_ALLOW_DAILY_PUSH_PARAMETERS_KEY, value)
}

/**
 * The user wants or not to receive a daily monthly notification when report is available
 *
 * @return true if we can display monthly notifications, false otherwise
 */
fun Parameters.isUserAllowingMonthlyReminderPushes(): Boolean {
    return getBoolean(USER_ALLOW_MONTHLY_PUSH_PARAMETERS_KEY, true)
}

/**
 * Set the user choice about monthly reminder notifications
 *
 * @param value if the user wants or not to receive monthly notifications
 */
fun Parameters.setUserAllowMonthlyReminderPushes(value: Boolean) {
    putBoolean(USER_ALLOW_MONTHLY_PUSH_PARAMETERS_KEY, value)
}

/**
 * Has the user complete the rating popup
 *
 * @return true if the user has already answered, false otherwise
 */
fun Parameters.hasUserCompleteRating(): Boolean {
    return getBoolean(RATING_COMPLETED_PARAMETERS_KEY, false)
}

/**
 * Set that the user has complete the rating popup process
 */
fun Parameters.setUserHasCompleteRating() {
    putBoolean(RATING_COMPLETED_PARAMETERS_KEY, true)
}

/**
 * Has the user saw the monthly report hint so far
 *
 * @return true if the user saw it, false otherwise
 */
fun Parameters.hasUserSawMonthlyReportHint(): Boolean {
    return getBoolean(USER_SAW_MONTHLY_REPORT_HINT_PARAMETERS_KEY, false)
}

/**
 * Set that the user saw the monthly report hint
 */
fun Parameters.setUserSawMonthlyReportHint() {
    putBoolean(USER_SAW_MONTHLY_REPORT_HINT_PARAMETERS_KEY, true)
}

fun Parameters.getTheme(): AppTheme {
    val value = getInt(APP_THEME_PARAMETERS_KEY, AppTheme.LIGHT.value)
    return AppTheme.entries.first { it.value == value }
}

fun Parameters.setTheme(theme: AppTheme) {
    putInt(APP_THEME_PARAMETERS_KEY, theme.value)
}

fun Parameters.isBackupEnabled(): Boolean {
    return getBoolean(BACKUP_ENABLED_PARAMETERS_KEY, false)
}

fun Parameters.setBackupEnabled(enabled: Boolean) {
    putBoolean(BACKUP_ENABLED_PARAMETERS_KEY, enabled)
}

fun Parameters.getLastBackupDate(): Date? {
    val lastTimestamp = getLong(LAST_BACKUP_TIMESTAMP, -1)
    if( lastTimestamp > 0 ) {
        return Date(lastTimestamp)
    }

    return null
}

fun Parameters.saveLastBackupDate(date: Date?) {
    if( date != null ) {
        putLong(LAST_BACKUP_TIMESTAMP, date.time)
    } else {
        putLong(LAST_BACKUP_TIMESTAMP, -1)
    }
}

fun Parameters.setShouldResetInitDate(shouldResetInitDate: Boolean) {
    putBoolean(SHOULD_RESET_INIT_DATE, shouldResetInitDate, forceCommit = true)
}

fun Parameters.getShouldResetInitDate(): Boolean {
    return getBoolean(SHOULD_RESET_INIT_DATE, false)
}

private lateinit var shouldShowCheckedBalanceFlow: MutableStateFlow<Boolean>

fun Parameters.watchShouldShowCheckedBalance(): StateFlow<Boolean> {
    if (!::shouldShowCheckedBalanceFlow.isInitialized) {
        shouldShowCheckedBalanceFlow = MutableStateFlow(getShouldShowCheckedBalance())
    }

    return shouldShowCheckedBalanceFlow
}

fun Parameters.getShouldShowCheckedBalance(): Boolean {
    return getBoolean(SHOULD_SHOW_CHECKED_BALANCE, false)
}

fun Parameters.setShouldShowCheckedBalance(shouldShow: Boolean) {
    if (!::shouldShowCheckedBalanceFlow.isInitialized) {
        shouldShowCheckedBalanceFlow = MutableStateFlow(shouldShow)
    }

    shouldShowCheckedBalanceFlow.value = shouldShow
    putBoolean(SHOULD_SHOW_CHECKED_BALANCE, shouldShow)
}

fun Parameters.getLatestSelectedOnlineAccountId(): String? {
    return getString(SELECTED_ACCOUNT_ID_KEY)
}

fun Parameters.setLatestSelectedOnlineAccountId(accountId: String?) {
    if (accountId != null) {
        putString(SELECTED_ACCOUNT_ID_KEY, accountId)
    } else {
        remove(SELECTED_ACCOUNT_ID_KEY)
    }
}