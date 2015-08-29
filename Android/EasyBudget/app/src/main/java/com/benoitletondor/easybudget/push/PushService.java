package com.benoitletondor.easybudget.push;

import android.app.IntentService;
import android.content.Intent;

import com.batch.android.Batch;
import com.benoitletondor.easybudget.BuildConfig;
import com.benoitletondor.easybudget.helper.Logger;

/**
 * Service that handles Batch pushes
 *
 * @author Benoit LETONDOR
 */
public class PushService extends IntentService
{
    /**
     * Key to retrieve the max version for a push
     */
    private final static String INTENT_MAX_VERSION_KEY = "maxVersion";
    /**
     * Key to retrieve the max version for a push
     */
    private final static String INTENT_MIN_VERSION_KEY = "minVersion";

// ----------------------------------->

    public PushService()
    {
        super("EasyBudgetPushService");
    }

// ----------------------------------->

    @Override
    protected void onHandleIntent(Intent intent)
    {
        try
        {
            if (Batch.Push.shouldDisplayPush(this, intent)) // Check that the push is valid
            {
                if( !shouldDisplayPush(intent) )
                {
                    Logger.debug("Not displaying push cause app version is not matching");
                    return;
                }

                // Display the notification
                Batch.Push.displayNotification(this, intent);
            }
        }
        finally
        {
            PushReceiver.completeWakefulIntent(intent);
        }
    }

    /**
     * Check if the push should be displayed according to version constrains
     *
     * @param intent
     * @return true if should display the push, false otherwise
     */
    private boolean shouldDisplayPush(Intent intent)
    {
        try
        {
            int maxVersion = BuildConfig.VERSION_CODE;
            int minVersion = 1;

            if( intent.hasExtra(INTENT_MAX_VERSION_KEY) )
            {
                maxVersion = Integer.parseInt(intent.getStringExtra(INTENT_MAX_VERSION_KEY));
            }

            if( intent.hasExtra(INTENT_MIN_VERSION_KEY) )
            {
                minVersion = Integer.parseInt(intent.getStringExtra(INTENT_MIN_VERSION_KEY));
            }

            return BuildConfig.VERSION_CODE <= maxVersion && BuildConfig.VERSION_CODE >= minVersion;
        }
        catch(Exception e)
        {
            Logger.error("Error while checking app version for push", e);
            return false;
        }
    }
}
