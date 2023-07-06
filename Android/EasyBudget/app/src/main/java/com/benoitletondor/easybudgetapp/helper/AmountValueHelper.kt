package com.benoitletondor.easybudgetapp.helper

import com.benoitletondor.easybudgetapp.BuildConfig
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Return the integer value of the double * 100 to store it as integer in DB. This is an ugly
 * method that shouldn't be there but rounding on doubles are a pain :/
 *
 * @return the corresponding int value (double * 100)
 */
fun Double.getDBValue(): Long {
    val stringValue = CurrencyHelper.getFormattedAmountValue(this)
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble: $stringValue")
    }

    val ceiledValue = ceil(this * 100).toLong()
    val ceiledDoubleValue = ceiledValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(ceiledDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return ceiled value: $ceiledValue")
        }
        return ceiledValue
    }

    val normalValue = this.toLong() * 100
    val normalDoubleValue = normalValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(normalDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return normal value: $normalValue")
        }
        return normalValue
    }

    val flooredValue = floor(this * 100).toLong()
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble, return floored value: $flooredValue")
    }

    return flooredValue
}

fun Long?.getRealValueFromDB(): Double = if( this != null ) this / 100.0 else 0.0