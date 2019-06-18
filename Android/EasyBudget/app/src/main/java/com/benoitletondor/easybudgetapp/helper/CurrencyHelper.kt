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

package com.benoitletondor.easybudgetapp.helper

import android.os.Build

import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.parameters.Parameters

import java.text.NumberFormat
import java.util.ArrayList
import java.util.Currency
import java.util.HashSet
import java.util.Locale

/**
 * Helper to work with currencies and display
 *
 * @author Benoit LETONDOR
 */
object CurrencyHelper {
    /**
     * List of main currencies ISO 4217 code
     */
    private val MAIN_CURRENCIES = arrayOf("USD", "EUR", "GBP", "IRN", "AUD", "CAD", "SGD", "CHF", "MYR", "JPY", "CNY", "NZD")

// ----------------------------------------->

    /**
     * Return a list of available main currencies based on [.MAIN_CURRENCIES] codes
     *
     * @return a list of currencies
     */
    fun getMainAvailableCurrencies(): List<Currency> {
        val mainCurrencies = ArrayList<Currency>(MAIN_CURRENCIES.size)

        for (currencyCode in MAIN_CURRENCIES) {
            try {
                val currency = Currency.getInstance(currencyCode)
                if (currency != null) {
                    mainCurrencies.add(currency)
                }
            } catch (e: Exception) {
                Logger.debug("Unable to find currency with code: $currencyCode")
            }

        }

        return mainCurrencies
    }

    /**
     * Return a list of available currencies (using compat code) minus main ones
     *
     * @return a list of other available currencies
     */
    fun getOtherAvailableCurrencies(): List<Currency> {
        val mainCurrencies = getMainAvailableCurrencies()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val currencies = ArrayList(Currency.getAvailableCurrencies())
            val currencyIterator = currencies.iterator()
            while (currencyIterator.hasNext()) {
                val currency = currencyIterator.next()

                if (mainCurrencies.contains(currency)) {
                    currencyIterator.remove()
                }
            }

            return currencies
        } else {
            val currencySet = HashSet<Currency>()

            val locales = Locale.getAvailableLocales()
            for (locale in locales) {
                try {
                    val currency = Currency.getInstance(locale)

                    if (mainCurrencies.contains(currency)) {
                        continue
                    }

                    currencySet.add(currency)
                } catch (ignored: Exception) { }
            }

            val currencies = ArrayList(currencySet)
            currencies.sortWith(Comparator { lhs, rhs -> lhs.currencyCode.compareTo(rhs.currencyCode) })

            return currencies
        }
    }

    /**
     * Get the currency display name (using compat)
     */
    fun getCurrencyDisplayName(currency: Currency): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            currency.symbol + " - " + currency.displayName
        } else {
            if (currency.symbol != currency.currencyCode) {
                currency.symbol + " - " + currency.currencyCode
            } else {
                currency.symbol
            }
        }
    }

    /**
     * Helper to display an amount using the user currency
     */
    @JvmStatic
    fun getFormattedCurrencyString(parameters: Parameters, amount: Double): String {
        val currencyFormat = NumberFormat.getCurrencyInstance()

        // No fraction digits
        currencyFormat.maximumFractionDigits = 2
        currencyFormat.minimumFractionDigits = 2

        currencyFormat.currency = parameters.getUserCurrency()

        return currencyFormat.format(amount)
    }

    /**
     * Helper to display an amount into an edit text
     */
    fun getFormattedAmountValue(amount: Double): String {
        val format = NumberFormat.getInstance()

        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
        format.isGroupingUsed = false

        return format.format(amount)
    }

    /**
     * Return the integer value of the double * 100 to store it as integer in DB. This is an ugly
     * method that shouldn't be there but rounding on doubles are a pain :/
     *
     * @param value the double value
     * @return the corresponding int value (double * 100)
     */
    fun getDBValueForDouble(value: Double): Long {
        val stringValue = getFormattedAmountValue(value)
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble: $stringValue")
        }

        val ceiledValue = Math.ceil(value * 100).toLong()
        val ceiledDoubleValue = ceiledValue / 100.0

        if (getFormattedAmountValue(ceiledDoubleValue) == stringValue) {
            if (BuildConfig.DEBUG_LOG) {
                Logger.debug("getDBValueForDouble, return ceiled value: $ceiledValue")
            }
            return ceiledValue
        }

        val normalValue = value.toLong() * 100
        val normalDoubleValue = normalValue / 100.0

        if (getFormattedAmountValue(normalDoubleValue) == stringValue) {
            if (BuildConfig.DEBUG_LOG) {
                Logger.debug("getDBValueForDouble, return normal value: $normalValue")
            }
            return normalValue
        }

        val flooredValue = Math.floor(value * 100).toLong()
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return floored value: $flooredValue")
        }

        return flooredValue
    }
}

/**
 * The chosen ISO code of the currency (string)
 */
private const val CURRENCY_ISO_PARAMETERS_KEY = "currency_iso"

fun Parameters.getUserCurrency(): Currency
    = Currency.getInstance(getString(CURRENCY_ISO_PARAMETERS_KEY))

fun Parameters.setUserCurrency(currency: Currency) {
    putString(CURRENCY_ISO_PARAMETERS_KEY, currency.currencyCode)
}
