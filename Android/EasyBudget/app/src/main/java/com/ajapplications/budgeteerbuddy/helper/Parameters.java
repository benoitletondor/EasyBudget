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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    private final SharedPreferences preferences;

    /**
     *
     * @param context
     */
    private Parameters(@NonNull Context context)
    {
        preferences = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

// --------------------------------------->

    /**
     * Save an integer for the given key
     *
     * @param key
     * @param value
     */
    public void putInt(@NonNull String key, int value)
    {
        preferences.edit().putInt(key, value).apply();
    }

    /**
     * Save a long for the given key
     *
     * @param key
     * @param value
     */
    public void putLong(@NonNull String key, long value)
    {
        preferences.edit().putLong(key, value).apply();
    }

    /**
     * Save a string for the given key
     *
     * @param key
     * @param value
     */
    public void putString(@NonNull String key, @NonNull String value)
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
    public int getInt(@NonNull String key, int defaultValue)
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
    public long getLong(@NonNull String key, long defaultValue)
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
    public boolean getBoolean(@NonNull String key, boolean defaultValue)
    {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Get the string value, returns null on not found
     *
     * @param key
     * @return
     */
    @Nullable
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
