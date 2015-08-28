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
     * Key to retrieve the new version for an update push
     */
    private final static String UPDATE_INTENT_VERSION_KEY = "newVersion";

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
            if ( Batch.Push.shouldDisplayPush(this, intent) ) // Check that the push is valid
            {
                if( isUpdatePush(intent) && !shouldDisplayUpdatePush(intent) )
                {
                    Logger.debug("Not displaying update push cause app is already updated");
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
     * Check if the given intent is an update push
     *
     * @param intent
     * @return true if intent contains the key, false otherwise
     */
    private boolean isUpdatePush(Intent intent)
    {
        return intent.hasExtra(UPDATE_INTENT_VERSION_KEY);
    }

    /**
     * Check if the version received in push is > to the current version
     *
     * @param intent
     * @return true if should display the push, false otherwise
     */
    private boolean shouldDisplayUpdatePush(Intent intent)
    {
        try
        {
            int newVersion = Integer.parseInt(intent.getStringExtra(UPDATE_INTENT_VERSION_KEY));

            return newVersion > BuildConfig.VERSION_CODE;
        }
        catch(Exception e)
        {
            Logger.error("Error while getting new version value from push", e);
            return false;
        }
    }
}
