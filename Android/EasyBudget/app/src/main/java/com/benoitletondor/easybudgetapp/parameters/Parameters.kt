/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.parameters

import android.content.Context
import android.content.SharedPreferences

/**
 * Name of the shared preferences file
 */
private const val SHARED_PREFERENCES_FILE_NAME = "easybudget_sp"

/**
 * Manage parameters into the app (wrapper of SharedPreferences).
 *
 * @author Benoit LETONDOR
 */
class Parameters(context: Context) {

    /**
     * Instance of shared preferences
     */
    private val preferences: SharedPreferences
        = context.applicationContext.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

// --------------------------------------->

    /**
     * Save an integer for the given key
     *
     * @param key
     * @param value
     */
    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    /**
     * Save a long for the given key
     *
     * @param key
     * @param value
     */
    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    /**
     * Save a string for the given key
     *
     * @param key
     * @param value
     */
    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    /**
     * Save a boolean for the given key
     *
     * @param key
     * @param value
     */
    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Get the integer value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    /**
     * Get the long value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    /**
     * Get the boolean value and fallback on default if not found
     *
     * @param key
     * @param defaultValue
     * @return
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    /**
     * Get the string value, returns null on not found
     *
     * @param key
     * @return
     */
    fun getString(key: String): String? {
        return preferences.getString(key, null)
    }
}
