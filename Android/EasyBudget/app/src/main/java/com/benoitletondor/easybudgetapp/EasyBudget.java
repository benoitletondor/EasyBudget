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

package com.benoitletondor.easybudgetapp;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.batch.android.Batch;
import com.batch.android.BatchUnlockListener;
import com.batch.android.Config;
import com.batch.android.Offer;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;

import com.benoitletondor.easybudgetapp.helper.UserHelper;
import com.benoitletondor.easybudgetapp.view.MainActivity;
import com.benoitletondor.easybudgetapp.view.RatingPopup;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.Logger.LogLevel;

import io.fabric.sdk.android.Fabric;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * EasyBudget application
 *
 * @author Benoit LETONDOR
 */
public class EasyBudget extends Application
{
    /**
     * Default amount use for low money warning (can be changed in settings)
     */
    public static final int DEFAULT_LOW_MONEY_WARNING_AMOUNT = 100;

// ------------------------------------------>

    private Tracker analyticsTracker;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Init actions
        init();

        // Check if an update occured and perform action if needed
        checkUpdateAction();

        // Crashlytics
        if( BuildConfig.CRASHLYTICS_ACTIVATED )
        {
            Fabric.with(this, new Crashlytics());

            Crashlytics.setUserIdentifier(Parameters.getInstance(getApplicationContext()).getString(ParameterKeys.LOCAL_ID));
        }

        // Batch
        setUpBatchSDK();

        // Analytics
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.setDryRun(!BuildConfig.ANALYTICS_ACTIVATED);
        if( BuildConfig.DEBUG_LOG )
        {
            analytics.getLogger().setLogLevel(LogLevel.VERBOSE);
        }

        analyticsTracker = analytics.newTracker(R.xml.analytics);
    }

    /**
     * Track that user comes from the given invitation id
     *
     * @param invitationId
     */
    public void trackInvitationId(String invitationId)
    {
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder()
                .setCustomDimension(1, "referral-appinvites")
                .build());
    }

    /**
     * Track the number of invites sent by the user
     *
     * @param invitationsSent
     */
    public void trackNumberOfInvitsSent(int invitationsSent)
    {
        int invitSent = Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.NUMBER_OF_INVITATIONS, 0);
        invitSent += invitationsSent;
        Parameters.getInstance(getApplicationContext()).putInt(ParameterKeys.NUMBER_OF_INVITATIONS, invitSent);

        analyticsTracker.send(new HitBuilders.ScreenViewBuilder()
                .setCustomMetric(1, (float) invitSent)
                .build());
    }

    /**
     * Init app const and parameters
     */
    private void init()
    {
        /*
         * Save first launch date if needed
         */
        long initDate = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.INIT_DATE, 0);
        if( initDate <= 0 )
        {
            Logger.debug("Registering first launch date");

            Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.INIT_DATE, new Date().getTime());
            CurrencyHelper.setUserCurrency(this, Currency.getInstance(Locale.getDefault())); // Set a default currency before onboarding
        }

        /*
         * Create local ID if needed
         */
        String localId = Parameters.getInstance(getApplicationContext()).getString(ParameterKeys.LOCAL_ID);
        if( localId == null )
        {
            localId = UUID.randomUUID().toString();
            Logger.debug("Generating local id : "+localId);

            Parameters.getInstance(getApplicationContext()).putString(ParameterKeys.LOCAL_ID, localId);
        }
        else
        {
            Logger.debug("Local id : " + localId);
        }

        // Activity counter for app foreground & background
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
        {
            private int activityCounter = 0;

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(Activity activity)
            {
                if (activityCounter == 0)
                {
                    onAppForeground(activity);
                }

                activityCounter++;
            }

            @Override
            public void onActivityResumed(Activity activity)
            {

            }

            @Override
            public void onActivityPaused(Activity activity)
            {

            }

            @Override
            public void onActivityStopped(Activity activity)
            {
                if (activityCounter == 1)
                {
                    onAppBackground();
                }

                activityCounter--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {

            }

            @Override
            public void onActivityDestroyed(Activity activity)
            {

            }
        });
    }

    /**
     * Show the rating popup if the user didn't asked not to every day after the app has been open
     * in 3 different days.
     *
     * @param activity
     */
    private void showRatingPopupIfNeeded(@NonNull Activity activity)
    {
        try
        {
            if( !(activity instanceof MainActivity) )
            {
                Logger.debug("Not showing rating popup cause app is not opened by the MainActivity");
                return;
            }

            int dailyOpens = Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, 0);
            if( dailyOpens > 2 )
            {
                long lastRatingTS = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, 0);
                if( lastRatingTS > 0 )
                {
                    Calendar cal = Calendar.getInstance();
                    int currentDay = cal.get(Calendar.DAY_OF_YEAR);

                    cal.setTime(new Date(lastRatingTS));
                    int lastTimeDay = cal.get(Calendar.DAY_OF_YEAR);

                    if( currentDay != lastTimeDay )
                    {
                        Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, new Date().getTime());
                        new RatingPopup(activity).show(false);
                    }
                }
                else
                {
                    Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, new Date().getTime());
                    new RatingPopup(activity).show(false);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error("Error while showing rating popup", e);
        }
    }

    /**
     * Set-up Batch SDK config + lifecycle
     */
    private void setUpBatchSDK()
    {
        Batch.setConfig(new Config(BuildConfig.BATCH_API_KEY));
        Batch.Push.setGCMSenderId("540863873711");
        Batch.Push.setManualDisplay(true);
        Batch.Push.setSmallIconResourceId(R.drawable.ic_push);
        Batch.Push.setNotificationsColor(ContextCompat.getColor(this, R.color.accent));

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(final Activity activity)
            {
                Batch.Unlock.setUnlockListener(new BatchUnlockListener()
                {
                    @Override
                    public void onRedeemAutomaticOffer(Offer offer)
                    {
                        boolean shouldShowPopup = true;

                        if ( offer.containsFeature(UserHelper.PREMIUM_FEATURE) )
                        {
                            boolean alreadyPremium =  Parameters.getInstance(getApplicationContext()).getBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, false);
                            if( alreadyPremium ) // Not show popup again if user is already premium
                            {
                                shouldShowPopup = false;
                            }
                            else
                            {
                                Parameters.getInstance(getApplicationContext()).putBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, true);
                            }
                        }

                        if( shouldShowPopup )
                        {
                            Map<String, String> additionalParameters = offer.getOfferAdditionalParameters();

                            String rewardMessage = additionalParameters.get("reward_message");
                            String rewardTitle = additionalParameters.get("reward_title");

                            if (rewardTitle != null && rewardMessage != null)
                            {
                                new AlertDialog.Builder(activity)
                                        .setTitle(rewardTitle)
                                        .setMessage(rewardMessage)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                            }
                        }
                    }
                });

                Batch.onStart(activity);
            }

            @Override
            public void onActivityResumed(Activity activity)
            {

            }

            @Override
            public void onActivityPaused(Activity activity)
            {

            }

            @Override
            public void onActivityStopped(Activity activity)
            {
                Batch.onStop(activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {

            }

            @Override
            public void onActivityDestroyed(Activity activity)
            {
                Batch.onDestroy(activity);
            }
        });
    }

    /**
     * Check if a an update occured and call {@link #onUpdate(int, int)} if so
     */
    private void checkUpdateAction()
    {
        int savedVersion = Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.APP_VERSION, 0);
        if( savedVersion > 0 && savedVersion != BuildConfig.VERSION_CODE )
        {
            onUpdate(savedVersion, BuildConfig.VERSION_CODE);
        }

        Parameters.getInstance(getApplicationContext()).putInt(ParameterKeys.APP_VERSION, BuildConfig.VERSION_CODE);
    }

    /**
     * Called when an update occurred
     *
     * @param previousVersion
     * @param newVersion
     */
    private void onUpdate(int previousVersion, int newVersion)
    {
        Logger.debug("Update detected, from " + previousVersion + " to " + newVersion);

        // Fix bad save of Batch premium before 1.1
        if( previousVersion <= 23 ) // 1.0.3
        {
            Parameters.getInstance(getApplicationContext()).putBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, true);
        }
    }

// -------------------------------------->

    /**
     * Called when the app goes foreground
     *
     * @param activity The activity that gone foreground
     */
    private void onAppForeground(@NonNull Activity activity)
    {
        Logger.debug("onAppForeground");

        /*
         * Increment the number of open
         */
        Parameters.getInstance(getApplicationContext()).putInt(ParameterKeys.NUMBER_OF_OPEN, Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.NUMBER_OF_OPEN, 0) + 1);

        /*
         * Check if last open is from another day
         */
        boolean shouldIncrementDailyOpen = false;

        long lastOpen = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.LAST_OPEN_DATE, 0);
        if( lastOpen > 0 )
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(lastOpen));

            int lastDay = cal.get(Calendar.DAY_OF_YEAR);

            cal.setTime(new Date());
            int currentDay = cal.get(Calendar.DAY_OF_YEAR);

            if( lastDay != currentDay )
            {
                shouldIncrementDailyOpen = true;
            }
        }
        else
        {
            shouldIncrementDailyOpen = true;
        }

        // Increment daily open
        if( shouldIncrementDailyOpen )
        {
            Parameters.getInstance(getApplicationContext()).putInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, Parameters.getInstance(getApplicationContext()).getInt(ParameterKeys.NUMBER_OF_DAILY_OPEN, 0) + 1);
        }

        /*
         * Save last open date
         */
        Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.LAST_OPEN_DATE, new Date().getTime());

        /*
         * Rating popup every day after 3 opens
         */
        showRatingPopupIfNeeded(activity);
    }

    /**
     * Called when the app goes background
     */
    private void onAppBackground()
    {
        Logger.debug("onAppBackground");
    }
}
