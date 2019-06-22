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