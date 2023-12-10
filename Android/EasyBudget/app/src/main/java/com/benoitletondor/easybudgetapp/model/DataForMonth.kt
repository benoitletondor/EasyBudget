package com.benoitletondor.easybudgetapp.model

import java.time.LocalDate
import java.time.YearMonth

data class DataForMonth(
    val month: YearMonth,
    val daysData: Map<LocalDate, DataForDay>,
) {
    companion object {
        const val numberOfLeewayDays: Long = 6
    }
}

data class DataForDay(
    val day: LocalDate,
    val expenses: List<Expense>,
    val balance: Double,
    val checkedBalance: Double,
)
