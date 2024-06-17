package com.benoitletondor.easybudgetapp.helper

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.YearMonth

@Serializable
@Parcelize
data class SerializedYearMonth(val year: Int, val month: Int) : Parcelable {
    constructor(yearMonth: YearMonth) : this(yearMonth.year, yearMonth.monthValue)

    fun toYearMonth(): YearMonth = YearMonth.of(year, month)
}

fun YearMonth.toSerializedYearMonth() = SerializedYearMonth(this)