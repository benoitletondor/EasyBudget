/*
 *   Copyright 2024 Benoit LETONDOR
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
package com.benoitletondor.easybudgetapp.helper

import android.util.Log
import com.benoitletondor.easybudgetapp.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

object Logger {
    private const val defaultTag = "EasyBudget"

    fun debug(message: String) {
        debug(message, error = null)
    }

    fun debug(message: String, error: Throwable?) {
        debug(defaultTag, message, error)
    }

    fun debug(tag: String, message: String) {
        debug(tag, message, error = null)
    }

    fun debug(tag: String, message: String, error: Throwable?) {
        if (BuildConfig.DEBUG_LOG) {
            Log.d(tag, message, error)
        }

        if (BuildConfig.CRASHLYTICS_ACTIVATED) {
            FirebaseCrashlytics.getInstance().log("D/$tag: $message")

            if (error != null) {
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }

    fun warning(message: String) {
        warning(message, error = null)
    }

    fun warning(message: String, error: Throwable?) {
        warning(defaultTag, message, error)
    }

    fun warning(tag: String, message: String) {
        warning(tag, message, error = null)
    }

    fun warning(tag: String, message: String, error: Throwable?) {
        Log.w(tag, message, error)

        if (BuildConfig.CRASHLYTICS_ACTIVATED) {
            FirebaseCrashlytics.getInstance().log("W/$tag: $message")

            if (error != null && !error.isNetworkError()) {
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }

    fun error(message: String) {
        error(message, error = null)
    }

    fun error(message: String, error: Throwable?) {
        error(defaultTag, message, error)
    }

    fun error(tag: String, message: String) {
        error(tag, message, error = null)
    }

    fun error(tag: String, message: String, error: Throwable?) {
        Log.e(tag, message, error)

        if (BuildConfig.CRASHLYTICS_ACTIVATED) {
            FirebaseCrashlytics.getInstance().log("E/$tag: $message")

            if (error != null && !error.isNetworkError()) {
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }
}