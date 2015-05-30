package com.benoitletondor.easybudget.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Singleton to manage parameters into the app (wrapper of SharedPreferences).
 *
 * @author Benoit LETONDOR
 */
public class Parameters
{
    /**
     * Name of the shared preferences file
     */
    private final static String SHARED_PREFERENCES_FILE_NAME = "easybudget_sp";

// --------------------------------------->

    /**
     * Instance of shared preferences
     */
    private SharedPreferences preferences;

    /**
     *
     * @param context
     */
    private Parameters(Context context)
    {
        if (context == null)
        {
            throw new NullPointerException("context==null");
        }

        preferences = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

// --------------------------------------->

    /**
     * Save an integer for the given key
     *
     * @param key
     * @param value
     */
    public void putInt(String key, int value)
    {
        preferences.edit().putInt(key, value).apply();
    }

    /**
     * Save a long for the given key
     *
     * @param key
     * @param value
     */
    public void putLong(String key, long value)
    {
        preferences.edit().putLong(key, value).apply();
    }

    /**
     * Save a string for the given key
     *
     * @param key
     * @param value
     */
    public void putString(String key, String value)
    {
        preferences.edit().putString(key, value).apply();
    }

    /**
     * Save a boolean for the given key
     *
     * @param key
     * @param value
     */
    public void putBoolean(String key, boolean value)
    {
        preferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Get the integer value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public int getInt(String key, int defaultValue)
    {
        return preferences.getInt(key, defaultValue);
    }

    /**
     * Get the long value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public long getLong(String key, long defaultValue)
    {
        return preferences.getLong(key, defaultValue);
    }

    /**
     * Get the boolean value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public Boolean getBoolean(String key, boolean defaultValue)
    {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Get the string value, returns null on not found
     *
     * @param key
     * @return
     */
    public String getString(String key)
    {
        return preferences.getString(key, null);
    }

// --------------------------------------->

    /**
     * Singleton instance
     */
    private static Parameters ourInstance;

    /**
     * Singleton getter
     *
     * @param context
     * @return
     */
    public static synchronized Parameters getInstance(Context context)
    {
        if (ourInstance == null)
        {
            ourInstance = new Parameters(context);
        }

        return ourInstance;
    }
}
