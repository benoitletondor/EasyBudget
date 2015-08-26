package com.benoitletondor.easybudget;

import android.app.Application;

import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.helper.ParameterKeys;
import com.benoitletondor.easybudget.helper.Parameters;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.util.Date;
import java.util.UUID;

/**
 * EasyBudget application
 *
 * @author Benoit LETONDOR
 */
public class EasyBudget extends Application
{
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
            Logger.debug("Local id : "+localId);
        }
    }
}
