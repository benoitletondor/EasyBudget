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

package com.ajapplications.budgeteerbuddy;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

//import com.batch.android.Batch;
//import com.batch.android.Config;
//import com.batch.android.PushNotificationType;
import com.ajapplications.budgeteerbuddy.iab.IabBroadcastReceiver;
import com.ajapplications.budgeteerbuddy.iab.IabHelper;
import com.ajapplications.budgeteerbuddy.helper.CurrencyHelper;
import com.ajapplications.budgeteerbuddy.helper.Logger;
import com.ajapplications.budgeteerbuddy.helper.ParameterKeys;
import com.ajapplications.budgeteerbuddy.helper.Parameters;
import com.ajapplications.budgeteerbuddy.helper.UIHelper;
import com.ajapplications.budgeteerbuddy.helper.UserHelper;
import com.ajapplications.budgeteerbuddy.iab.IabResult;
import com.ajapplications.budgeteerbuddy.iab.Purchase;
import com.ajapplications.budgeteerbuddy.notif.DailyNotifOptinService;
import com.ajapplications.budgeteerbuddy.notif.MonthlyReportNotifService;
import com.ajapplications.budgeteerbuddy.view.MainActivity;
import com.ajapplications.budgeteerbuddy.view.RatingPopup;
import com.ajapplications.budgeteerbuddy.view.SettingsActivity;
//import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

//import io.fabric.sdk.android.Fabric;

/**
 * EasyBudget application. Implements GA tracking, Batch set-up, Crashlytics set-up && iab.
 *
 * @author Benoit LETONDOR
 */
public class EasyBudget extends Application implements IabBroadcastReceiver.IabBroadcastListener
{
    /**
     * Default amount use for low money warning (can be changed in settings)
     */
    public static final int DEFAULT_LOW_MONEY_WARNING_AMOUNT = 100;
    /**
     * iab SDK used for premium
     */
    public static final String SKU_PREMIUM = "premium";
    /**
     * Intent action broadcast when the status of iab changed
     */
    public static final String INTENT_IAB_STATUS_CHANGED = "iabStatusChanged";
    /**
     * Key to retrieve the iab status on an {@link #INTENT_IAB_STATUS_CHANGED} intent
     */
    public static final String INTENT_IAB_STATUS_KEY = "iabKey";

// ------------------------------------------>

    /**
     * GA tracker
     */
    private Tracker analyticsTracker;

    /**
     * Helper to work with iab
     */
    private IabHelper iabHelper;
    /**
     * iab Broadcast Receiver
     */
    private IabBroadcastReceiver iabBroadcastReceiver;
    /**
     * iab check status
     */
    private volatile PremiumCheckStatus iabStatus;
    /**
     * iab inventory listener
     */
    private IabHelper.QueryInventoryFinishedListener inventoryListener;
    /**
     * Last error received by iab
     */
    private String iabError;

// ------------------------------------------>

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Init actions
        init();

        // Check if an update occurred and perform action if needed
        checkUpdateAction();

        // Crashlytics
        if( BuildConfig.CRASHLYTICS_ACTIVATED )
        {
//            Fabric.with(this, new Crashlytics());
//
//            Crashlytics.setUserIdentifier(Parameters.getInstance(getApplicationContext()).getString(ParameterKeys.LOCAL_ID));
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

        // In-app billing
//        setupIab();
    }

    @Override
    public void onTerminate()
    {
        if (iabBroadcastReceiver != null)
        {
            unregisterReceiver(iabBroadcastReceiver);
        }

        super.onTerminate();
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
                if( !hasRatingPopupBeenShownToday() )
                {
                    boolean shown = new RatingPopup(activity).show(false);
                    if( shown )
                    {
                        Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, new Date().getTime());
                    }
                }
            }
        }
        catch (Exception e)
        {
            Logger.error("Error while showing rating popup", e);
        }
    }

    private void showPremiumPopupIfNeeded(@NonNull final Activity activity)
    {
        try
        {
            if( !(activity instanceof MainActivity) )
            {
                return;
            }

            if( Parameters.getInstance(getApplicationContext()).getBoolean(ParameterKeys.PREMIUM_POPUP_COMPLETE, false) )
            {
                return;
            }

            if( UserHelper.isUserPremium(this) )
            {
                return;
            }

            if( !UserHelper.hasUserCompleteRating(activity) )
            {
                return;
            }

            RatingPopup.RatingPopupStep currentStep = RatingPopup.getUserStep(activity);
            if( currentStep == RatingPopup.RatingPopupStep.STEP_LIKE ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_NOT_RATED ||
                    currentStep == RatingPopup.RatingPopupStep.STEP_LIKE_RATED )
            {
                if( !hasRatingPopupBeenShownToday() && shouldShowPremiumPopup() )
                {
                    Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.PREMIUM_POPUP_LAST_AUTO_SHOW, new Date().getTime());

                    AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle(R.string.premium_popup_become_title)
                        .setMessage(R.string.premium_popup_become_message)
                        .setPositiveButton(R.string.premium_popup_become_cta, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Intent startIntent = new Intent(activity, SettingsActivity.class);
                                startIntent.putExtra(SettingsActivity.SHOW_PREMIUM_INTENT_KEY, true);
                                ActivityCompat.startActivity(activity, startIntent, null);

                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.premium_popup_become_not_now, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.premium_popup_become_not_ask_again, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Parameters.getInstance(getApplicationContext()).putBoolean(ParameterKeys.PREMIUM_POPUP_COMPLETE, true);
                                dialog.dismiss();
                            }
                        })
                        .show();

                    UIHelper.centerDialogButtons(dialog);
                }
            }
        }
        catch (Exception e)
        {
            Logger.error("Error while showing become premium popup", e);
        }
    }

    /**
     * Has the rating popup been shown automatically today
     *
     * @return true if the rating popup has been shown today, false otherwise
     */
    private boolean hasRatingPopupBeenShownToday()
    {
        long lastRatingTS = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.RATING_POPUP_LAST_AUTO_SHOW, 0);
        if( lastRatingTS > 0 )
        {
            Calendar cal = Calendar.getInstance();
            int currentDay = cal.get(Calendar.DAY_OF_YEAR);

            cal.setTime(new Date(lastRatingTS));
            int lastTimeDay = cal.get(Calendar.DAY_OF_YEAR);

            return currentDay == lastTimeDay;
        }

        return false;
    }

    /**
     * Check that last time the premium popup was shown was 2 days ago or more
     *
     * @return true if we can show premium popup, false otherwise
     */
    private boolean shouldShowPremiumPopup()
    {
        long lastPremiumTS = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.PREMIUM_POPUP_LAST_AUTO_SHOW, 0);
        if( lastPremiumTS == 0 )
        {
            return true;
        }

        // Set calendar to last time 00:00 + 2 days
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(lastPremiumTS));
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, 2);

        return new Date().after(cal.getTime());
    }

    /**
     * Show the 1.5 app update notification
     */
    private void show1_5UpdateNotif()
    {
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getApplicationContext())
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(getResources().getString(R.string.app_name))
            .setContentText(getResources().getString(R.string.recurring_update_notification))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.recurring_update_notification)))
            .setColor(ContextCompat.getColor(getApplicationContext(), R.color.accent))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManagerCompat.from(getApplicationContext()).notify(4014, notifBuilder.build());
    }

    /**
     * Set-up Batch SDK config + lifecycle
     */
    private void setUpBatchSDK()
    {
//        Batch.setConfig(new Config(BuildConfig.BATCH_API_KEY));
//        Batch.Push.setGCMSenderId(BuildConfig.GCM_SENDER_ID);
//        Batch.Push.setManualDisplay(true);
//        Batch.Push.setSmallIconResourceId(R.drawable.ic_push);
//        Batch.Push.setNotificationsColor(ContextCompat.getColor(this, R.color.accent));
//
//        // Remove vibration & sound
//        EnumSet<PushNotificationType> notificationTypes = EnumSet.allOf(PushNotificationType.class);
//        notificationTypes.remove(PushNotificationType.VIBRATE);
//        notificationTypes.remove(PushNotificationType.SOUND);
//        Batch.Push.setNotificationsType(notificationTypes);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(final Activity activity)
            {
//                Batch.onStart(activity);
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
//                Batch.onStop(activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {

            }

            @Override
            public void onActivityDestroyed(Activity activity)
            {
//                Batch.onDestroy(activity);
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
        if( previousVersion <= BuildVersion.VERSION_1_1_3)
        {
            UserHelper.setBatchUserPremium(this);
        }

        if( newVersion == BuildVersion.VERSION_1_2 )
        {
            if( UserHelper.isUserPremium(this) && !DailyNotifOptinService.hasDailyReminderOptinNotifBeenShown(this) )
            {
                DailyNotifOptinService.showDailyReminderOptinNotif(getApplicationContext());
            }
        }

        if( newVersion == BuildVersion.VERSION_1_3 && !MonthlyReportNotifService.hasUserSeenMonthlyReportNotif(this) )
        {
            if( UserHelper.isUserPremium(this) )
            {
                MonthlyReportNotifService.showPremiumNotif(getApplicationContext());
            }
            else
            {
                MonthlyReportNotifService.showNotPremiumNotif(getApplicationContext());
            }
        }

        if( newVersion == BuildVersion.VERSION_1_5_2 )
        {
            show1_5UpdateNotif();
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

        /*
         * Premium popup after rating complete
         */
        showPremiumPopupIfNeeded(activity);

        /*
         * Update iap status if needed
         */
        updateIAPStatusIfNeeded();
    }

    /**
     * Called when the app goes background
     */
    private void onAppBackground()
    {
        Logger.debug("onAppBackground");
    }

// -------------------------------------->
    // region iab

//    private void setupIab()
//    {
//        try
//        {
//            setIabStatusAndNotify(PremiumCheckStatus.INITIALIZING);
//
//            iabHelper = new IabHelper(this, BuildConfig.LICENCE_KEY);
//            iabHelper.enableDebugLogging(BuildConfig.DEBUG_LOG);
//
//            inventoryListener = new IabHelper.QueryInventoryFinishedListener()
//            {
//                @Override
//                public void onQueryInventoryFinished(IabResult result, Inventory inventory)
//                {
//                    Logger.debug("iab query inventory finished.");
//
//                    // Is it a failure?
//                    if ( result.isFailure() )
//                    {
//                        Logger.error("Error while querying iab inventory: "+result);
//                        iabError = result.getMessage();
//                        setIabStatusAndNotify(PremiumCheckStatus.ERROR);
//                        return;
//                    }
//
//                    /*
//                     * Check for items we own.
//                     * TODO We should check the developer payload to see if it's correct verifyDeveloperPayload(premiumPurchase)!
//                     */
//                    Purchase premiumPurchase = inventory.getPurchase(EasyBudget.SKU_PREMIUM);
//                    setIabStatusAndNotify(premiumPurchase != null ? PremiumCheckStatus.PREMIUM : PremiumCheckStatus.NOT_PREMIUM);
//
//                    Logger.debug("iab query inventory was successful: "+iabStatus);
//                }
//            };
//
//            iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
//            {
//                public void onIabSetupFinished(IabResult result)
//                {
//                    Logger.debug("iab setup finished.");
//
//                    if (!result.isSuccess())
//                    {
//                        // Oh noes, there was a problem.
//                        setIabStatusAndNotify(PremiumCheckStatus.ERROR);
//                        Logger.error("Error while setting-up iab: "+result);
//                        iabError = result.getMessage();
//                        return;
//                    }
//
//                    setIabStatusAndNotify(PremiumCheckStatus.CHECKING);
//
//                    // Important: Dynamically register for broadcast messages about updated purchases.
//                    // We register the receiver here instead of as a <receiver> in the Manifest
//                    // because we always call getPurchases() at startup, so therefore we can ignore
//                    // any broadcasts sent while the app isn't running.
//                    iabBroadcastReceiver = new IabBroadcastReceiver(EasyBudget.this);
//                    IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
//                    registerReceiver(iabBroadcastReceiver, broadcastFilter);
//
//                    // IAB is fully set up. Now, let's get an inventory of stuff we own.
//                    Logger.debug("iab setup successful. Querying inventory.");
//                    iabHelper.queryInventoryAsync(inventoryListener);
//                }
//            });
//        }
//        catch (Exception e)
//        {
//            Logger.error("Error while checking iab status", e);
//            setIabStatusAndNotify(PremiumCheckStatus.ERROR);
//        }
//    }

    @Override
    public void receivedBroadcast()
    {
        // Received a broadcast notification that the inventory of items has changed
        Logger.debug("iab: Received broadcast notification. Querying inventory.");
        iabHelper.queryInventoryAsync(inventoryListener);
    }

    /**
     * Set the new iab status and notify the app by sending an {@link #INTENT_IAB_STATUS_CHANGED} intent
     *
     * @param status the new status
     */
    private void setIabStatusAndNotify(@NonNull PremiumCheckStatus status)
    {
        iabStatus = status;

        // Save status only on success
        if( status == PremiumCheckStatus.PREMIUM || status == PremiumCheckStatus.NOT_PREMIUM )
        {
            Parameters.getInstance(this).putBoolean(ParameterKeys.PREMIUM, iabStatus == PremiumCheckStatus.PREMIUM);
        }

        Intent intent = new Intent(INTENT_IAB_STATUS_CHANGED);
        intent.putExtra(INTENT_IAB_STATUS_KEY, status);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Update the current IAP status if already checked
     */
    private void updateIAPStatusIfNeeded()
    {
        Logger.debug("updateIAPStatusIfNeeded: "+iabStatus);

        if( iabStatus == PremiumCheckStatus.NOT_PREMIUM )
        {
            setIabStatusAndNotify(PremiumCheckStatus.CHECKING);
            iabHelper.queryInventoryAsync(inventoryListener);
        }
    }

    /**
     * Launch the redeem promocode flow
     *
     * @param promocode the promocode to redeem
     * @param activity the current activity
     */
    public boolean launchRedeemPromocodeFlow(@NonNull String promocode, @NonNull Activity activity)
    {
        try
        {
            String url = "https://play.google.com/redeem?code=" + URLEncoder.encode(promocode, "UTF-8");
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }
        catch (Exception e)
        {
            Logger.error(false, "Error while redeeming promocode", e);
            return false;
        }
    }

    /**
     * Launch the premium purchase flow
     *
     * @param activity activity that started this purchase
     * @param listener listener for purchase events
     */
    public void launchPremiumPurchaseFlow(final @NonNull Activity activity, final  @NonNull PremiumPurchaseListener listener)
    {
        if( iabStatus != PremiumCheckStatus.NOT_PREMIUM )
        {
            if( iabStatus == PremiumCheckStatus.ERROR )
            {
                listener.onPurchaseError(iabError);
            }
            else
            {
                listener.onPurchaseError("Runtime error: "+iabStatus);
            }

            return;
        }

        iabHelper.launchPurchaseFlow(activity, SKU_PREMIUM, 10001, new IabHelper.OnIabPurchaseFinishedListener()
        {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase purchase)
            {
                Logger.debug("Purchase finished: " + result + ", purchase: " + purchase);

                // if we were disposed of in the meantime, quit.
                if (iabHelper == null) return;

                if (result.isFailure())
                {
                    Logger.error("Error while purchasing premium: " + result);
                    if( result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED )
                    {
                        listener.onUserCancelled();
                    }
                    else
                    {
                        listener.onPurchaseError(result.getMessage());
                    }

                    return;
                }

                // TODO verify purchase using verifyDeveloperPayload(purchase)

                Logger.debug("Purchase successful.");

                if (purchase.getSku().equals(SKU_PREMIUM))
                {
                    iabStatus = PremiumCheckStatus.PREMIUM;
                    listener.onPurchaseSuccess();
                }
                else // Should not happen but just in case
                {
                    listener.onPurchaseError("Unknown SKU");
                }
            }
        }, null);
    }

    /**
     * Should be called by the activity calling {@link #launchPremiumPurchaseFlow(Activity, PremiumPurchaseListener)}
     * when receiving the {@link Activity#onActivityResult(int, int, Intent)} call. If this methods
     * returns true, the activity shouldn't do anything, not even calling super.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true if the event has been handled by iab, false if the activity should handle it
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data)
    {
        return iabHelper.handleActivityResult(requestCode, resultCode, data);
    }

    //endregion
}
