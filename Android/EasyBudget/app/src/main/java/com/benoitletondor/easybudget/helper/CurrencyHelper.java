package com.benoitletondor.easybudget.helper;

import android.content.Context;
import android.support.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;

/**
 * Helper to work with currencies and display
 *
 * @author Benoit LETONDOR
 */
public class CurrencyHelper
{
    /**
     * Helper to display an amount using the user currency<br />
     * http://speakman.net.nz/blog/2013/10/21/android-currency-localisation-hell/
     *
     * @param context
     * @param amount
     * @return
     */
    public static String getFormattedCurrencyString(@NonNull Context context, int amount)
    {
        // This formats currency values as the user expects to read them (default locale).
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

        String symbol = getUserCurrency(context).getSymbol();

        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }

    /**
     * Convenience method to get user currency
     *
     * @param context
     * @return
     */
    public static Currency getUserCurrency(@NonNull Context context)
    {
        return Currency.getInstance(Parameters.getInstance(context).getString(ParameterKeys.CURRENCY_ISO));
    }

    /**
     * Convenience method to set user currency
     *
     * @param context
     * @param currency
     */
    public static void setUserCurrency(@NonNull Context context, @NonNull Currency currency)
    {
        Parameters.getInstance(context).putString(ParameterKeys.CURRENCY_ISO, currency.getCurrencyCode());
    }
}
