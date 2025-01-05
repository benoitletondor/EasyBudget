/*
 *   Copyright 2025 Benoit Letondor
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

import com.benoitletondor.easybudgetapp.parameters.Parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.*

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
     * Return a list of available currencies minus main ones
     *
     * @return a list of other available currencies
     */
    fun getOtherAvailableCurrencies(): List<Currency> {
        val mainCurrencies = getMainAvailableCurrencies()

        return Currency.getAvailableCurrencies().filter { !mainCurrencies.contains(it) }
    }

    /**
     * Get the currency display name
     */
    fun getCurrencyDisplayName(currency: Currency): String
        = currency.symbol + " - " + currency.displayName

    fun getFormattedCurrencyString(currency: Currency, amount: Double): String {
        val currencyFormat = NumberFormat.getCurrencyInstance()

        // No fraction digits
        currencyFormat.maximumFractionDigits = 2
        currencyFormat.minimumFractionDigits = 2

        currencyFormat.currency = currency

        return currencyFormat.format(amount)
    }

    /**
     * Helper to display an amount using the user currency
     */
    fun getFormattedCurrencyString(parameters: Parameters, amount: Double): String {
        return getFormattedCurrencyString(parameters.getUserCurrency(), amount)
    }

    /**
     * Helper to display an amount into an edit text
     */
    fun getFormattedAmountValue(amount: Double): String {
        val format = NumberFormat.getInstance()

        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
        format.isGroupingUsed = false

        return format.format(amount).replace(",", ".")
    }
}

/**
 * The chosen ISO code of the currency (string)
 */
private const val CURRENCY_ISO_PARAMETERS_KEY = "currency_iso"

private lateinit var userCurrencyFlow: MutableStateFlow<Currency>

fun Parameters.watchUserCurrency(): StateFlow<Currency> {
    if (!::userCurrencyFlow.isInitialized) {
        userCurrencyFlow = MutableStateFlow(getUserCurrency())
    }

    return userCurrencyFlow
}

fun Parameters.getUserCurrency(): Currency
    = Currency.getInstance(getString(CURRENCY_ISO_PARAMETERS_KEY))

fun Parameters.setUserCurrency(currency: Currency) {
    if (!::userCurrencyFlow.isInitialized) {
        userCurrencyFlow = MutableStateFlow(currency)
    }

    userCurrencyFlow.value = currency
    putString(CURRENCY_ISO_PARAMETERS_KEY, currency.currencyCode)
}
