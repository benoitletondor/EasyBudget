package com.benoitletondor.easybudgetapp.model

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDate

data class AssociatedRecurringExpense(
    val recurringExpense: RecurringExpense,
    val originalDate: LocalDate,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(RecurringExpense::class.java.classLoader)!!,
        LocalDate.ofEpochDay(parcel.readLong()),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(recurringExpense, flags)
        parcel.writeLong(originalDate.toEpochDay())
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AssociatedRecurringExpense> {
        override fun createFromParcel(parcel: Parcel): AssociatedRecurringExpense {
            return AssociatedRecurringExpense(parcel)
        }

        override fun newArray(size: Int): Array<AssociatedRecurringExpense?> {
            return arrayOfNulls(size)
        }
    }
}
