package com.benoitletondor.easybudget.helper;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helper to work with currencies and display
 *
 * @author Benoit LETONDOR
 */
public class CurrencyHelper
{
    /**
     * Return a list of available currencies (using compat code)
     *
     * @return a list of available currencies
     */
    public static List<Currency> getAvailableCurrencies()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            return new ArrayList<>(Currency.getAvailableCurrencies());
        }
        else
        {
            Set<Currency> currencySet = new HashSet<>();

            Locale[] locales = Locale.getAvailableLocales();
            for(Locale locale : locales)
            {
                try
                {
                    Currency currency = Currency.getInstance(locale);
                    currencySet.add(currency);
                }
                catch(Exception e)
                {
                    // Locale not found
                }
            }

            List<Currency> currencies = new ArrayList<>(currencySet);
            Collections.sort(currencies, new Comparator<Currency>()
            {
                @Override
                public int compare(Currency lhs, Currency rhs)
                {
                    return lhs.getCurrencyCode().compareTo(rhs.getCurrencyCode());
                }
            });

            return currencies;
        }
    }

    /**
     * Get the currency display name (using compat)
     *
     * @param currency
     * @return
     */
    public static String getCurrencyDisplayName(Currency currency)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            return currency.getSymbol()+ " - "+ currency.getDisplayName();
        }
        else
        {
            if( !currency.getSymbol().equals(currency.getCurrencyCode()) )
            {
                return currency.getSymbol()+ " - "+ currency.getCurrencyCode();
            }
            else
            {
                return currency.getSymbol();
            }
        }
    }

    /**
     * Helper to display an amount using the user currency
     *
     * @param context
     * @param amount
     * @return
     */
    public static String getFormattedCurrencyString(@NonNull Context context, int amount)
    {
        // This formats currency values as the user expects to read them (default locale).
        DecimalFormat currencyFormat = (DecimalFormat) NumberFormat.getCurrencyInstance();

        // No fraction digits
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);

        String symbol = getUserCurrency(context).getSymbol();

        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);

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
