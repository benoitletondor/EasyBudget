package com.benoitletondor.easybudget.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author Benoit LETONDOR
 */
public class Parameters
{
    private final static String SHARED_PREFERENCES_FILE_NAME = "easybudget_sp";

// --------------------------------------->

    private SharedPreferences preferences;

    private Parameters(Context context)
    {
        if( context == null )
        {
            throw new NullPointerException("context==null");
        }

        preferences = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

// --------------------------------------->

    public void putInt(String key, int value)
    {
        preferences.edit().putInt(key, value).apply();
    }

    public void putString(String key, String value)
    {
        preferences.edit().putString(key, value).apply();
    }

    public void putBoolean(String key, boolean value)
    {
        preferences.edit().putBoolean(key, value).apply();
    }

    public int getInt(String key, int defaultValue)
    {
        return preferences.getInt(key, defaultValue);
    }

    public Boolean getBoolean(String key, boolean defaultValue)
    {
        return preferences.getBoolean(key, defaultValue);
    }

    public String getString(String key)
    {
        return preferences.getString(key, null);
    }

// --------------------------------------->

    private static Parameters ourInstance;

    public static synchronized Parameters getInstance(Context context)
    {
        if( ourInstance == null )
        {
            ourInstance = new Parameters(context);
        }

        return ourInstance;
    }
}
