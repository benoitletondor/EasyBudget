package com.benoitletondor.easybudget;

import android.app.Application;

import com.benoitletondor.easybudget.helper.Logger;
import com.benoitletondor.easybudget.helper.ParameterKeys;
import com.benoitletondor.easybudget.helper.Parameters;

import java.util.Date;

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

        firstLaunchActions();
    }

    private void firstLaunchActions()
    {
        long initDate = Parameters.getInstance(getApplicationContext()).getLong(ParameterKeys.INIT_DATE, 0);
        if( initDate <= 0 )
        {
            Logger.debug("First launch actions");

            Parameters.getInstance(getApplicationContext()).putLong(ParameterKeys.INIT_DATE, new Date().getTime());
        }
    }
}
