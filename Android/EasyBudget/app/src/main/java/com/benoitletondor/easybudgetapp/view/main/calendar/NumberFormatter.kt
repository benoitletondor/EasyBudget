package com.benoitletondor.easybudgetapp.view.main.calendar

import android.icu.number.Notation
import android.icu.text.CompactDecimalFormat
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

interface NumberFormatter {
    fun format(amount: Double): String

    companion object {
        fun get(): NumberFormatter = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> API30NumberFormatter()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> API24NumberFormatter()
            else -> RoundedToIntNumberFormatter()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private class API30NumberFormatter : NumberFormatter {
    private val formatter = android.icu.number.NumberFormatter.with()
        .notation(Notation.compactShort())
        .locale(Locale.getDefault())

    override fun format(amount: Double): String = formatter.format(amount).toString()
}

@RequiresApi(Build.VERSION_CODES.N)
private class API24NumberFormatter : NumberFormatter {
    private val formatter = CompactDecimalFormat.getInstance(
        Locale.getDefault(),
        CompactDecimalFormat.CompactStyle.SHORT,
    )

    override fun format(amount: Double): String = formatter.format(amount)
}

class RoundedToIntNumberFormatter : NumberFormatter {
    override fun format(amount: Double): String = amount.toInt().toString()
}