package com.benoitletondor.easybudget;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.benoitletondor.easybudget.helper.CurrencyHelper;
import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.helper.ParameterKeys;
import com.benoitletondor.easybudget.helper.Parameters;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.Logger.LogLevel;

import io.fabric.sdk.android.Fabric;

import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * EasyBudget application
 *
 * @author Benoit LETONDOR
 */
public class EasyBudget extends Application
{
    private Tracker analyticsTracker;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Init actions
        init();

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

            CurrencyHelper.setUserCurrency(this, Currency.getInstance(Locale.US)); //FIXME remove that
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
    }

    /**
     * Set-up Batch SDK config + lifecycle
     */
    private void setUpBatchSDK()
    {
        Batch.setConfig(new Config(BuildConfig.BATCH_API_KEY));
        Batch.Push.setGCMSenderId("540863873711");
        Batch.Push.setManualDisplay(true);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {

            }

            @Override
            public void onActivityStarted(Activity activity)
            {
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
}
