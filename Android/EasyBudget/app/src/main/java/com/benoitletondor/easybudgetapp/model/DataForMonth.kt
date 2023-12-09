package com.benoitletondor.easybudgetapp.model

import java.time.LocalDate
import java.time.YearMonth

data class DataForMonth(
    val month: YearMonth,
    val includesCheckedBalance: Boolean,
    val daysData: Map<LocalDate, DataForDay>,
) {
    companion object {
        val numberOfLeewayDays: Long = 6
    }
}

data class DataForDay(
    val day: LocalDate,
    val expenses: List<Expense>,
    val balance: Double,
    val maybeCheckedBalance: Double?,
)
