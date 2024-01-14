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

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * This is an exact mapping of the string array "themesData"
 */
enum class AppTheme(val value: Int) {
    LIGHT(0),
    DARK(1),
    PLATFORM_DEFAULT(2);

    fun toPlatformValue(): Int {
        return when(value) {
            LIGHT.value -> return AppCompatDelegate.MODE_NIGHT_NO
            DARK.value -> return AppCompatDelegate.MODE_NIGHT_YES
            PLATFORM_DEFAULT.value -> if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
    }
}