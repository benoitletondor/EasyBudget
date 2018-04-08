/*
 *   Copyright 2015 Benoit LETONDOR
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

package com.ajapplications.budgeteerbuddy.helper;

/**
 * List of keys to query {@link Parameters}
 *
 * @author Benoit LETONDOR
 */
public class ParameterKeys
{
    /**
     * App version stored to detect updates (int)
     */
    public static final String APP_VERSION = "appversion";
    /**
     * Date of the base balance set-up (long)
     */
    public static final String INIT_DATE       = "init_date";
    /**
     * Local identifier of the device (generated on first launch) (string)
     */
    public static final String LOCAL_ID        = "local_id";
    /**
     * The chosen ISO code of the currency (string)
     */
    public static final String CURRENCY_ISO    = "currency_iso";
    /**
     * Warning limit for low money on account (int)
     */
    public static final String LOW_MONEY_WARNING_AMOUNT = "low_money_warning_amount";
    /**
     * The current onboarding step (int)
     */
    public static final String ONBOARDING_STEP = "onboarding_step";
    /**
     * Are animations enabled (boolean)
     */
    public static final String ANIMATIONS_ENABLED = "animation_enabled";
    /**
     * Number of invitations sent by the user (int)
     */
    public static final String NUMBER_OF_INVITATIONS = "number_of_invitations";
    /**
     * ID of the invitation the user came with (String)
     */
    public static final String INVITATION_ID = "invitation_id";
    /**
     * Source of the installation (String)
     */
    public static final String INSTALLATION_SOURCE = "installation_source";
    /**
     * Referrer of the installation (String)
     */
    public static final String INSTALLATION_REFERRER = "installation_referrer";
    /**
     * Store the user step in the rating process (int)
     */
    public static final String RATING_STEP = "rating_step";
    /**
     * Has the user redeemed a Batch offer (bool)
     */
    public static final String BATCH_OFFER_REDEEMED = "batch_offer_redeemed";
    /**
     * Number of time the app has been opened (int)
     */
    public static final String NUMBER_OF_OPEN = "number_of_open";
    /**
     * Timestamp of the last open (long)
     */
    public static final String LAST_OPEN_DATE = "last_open_date";
    /**
     * Number of time different day has been open (int)
     */
    public static final String NUMBER_OF_DAILY_OPEN = "number_of_daily_open";
    /**
     * Indicate if the rating has been completed by the user (finished or not ask me again) (bool)
     */
    public static final String RATING_COMPLETED = "rating_completed";
    /**
     * Timestamp that indicates the last time user was presented the rating popup (long)
     */
    public static final String RATING_POPUP_LAST_AUTO_SHOW = "rating_popup_last_auto_show";
    /**
     * The user wants to receive notifications for updates (bool)
     */
    public static final String USER_ALLOW_UPDATE_PUSH = "user_allow_update_push";
    /**
     * The user wants to receive a daily reminder notification (bool)
     */
    public static final String USER_ALLOW_DAILY_PUSH = "user_allow_daily_push";
    /**
     * The user wants to receive a monthly reminder notification when report is available (bool)
     */
    public static final String USER_ALLOW_MONTHLY_PUSH = "user_allow_monthly_push";
    /**
     * Has the daily push opt-in been shown to the user yet (bool)
     */
    public static final String DAILY_PUSH_NOTIF_SHOWN = "user_saw_daily_push_notif";
    /**
     * Has the monthly report notification been shown to the user yet (bool)
     */
    public static final String MONTHLY_PUSH_NOTIF_SHOWN = "user_saw_monthly_push_notif";
    /**
     * Timestamp that indicates the last time user was presented the become premium popup (long)
     */
    public static final String PREMIUM_POPUP_LAST_AUTO_SHOW = "premium_popup_last_auto_show";
    /**
     * Has the user complete the premium popup = should we show it again or not (bool)
     */
    public static final String PREMIUM_POPUP_COMPLETE = "premium_popup_complete";
    /**
     * Has the user saw the monthly report hint (bool)
     */
    public static final String USER_SAW_MONTHLY_REPORT_HINT = "user_saw_monthly_report_hint";
    /**
     * Is the user premium from AppTurbo (bool)
     */
    public static final String APP_TURBO_PREMIUM = "appturbo_offer";
    /**
     * Cache storage of the IAB status
     */
    public static final String PREMIUM = "premium";
}
