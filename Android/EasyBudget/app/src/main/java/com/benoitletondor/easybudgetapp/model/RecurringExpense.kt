package com.benoitletondor.easybudgetapp.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class RecurringExpense(val id: Long?,
                            val title: String,
                            val originalAmount: Double,
                            val recurringDate: Date,
                            val modified: Boolean, // Not implemented yet
                            val type: RecurringExpenseType) : Parcelable {
    
    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()!!,
        parcel.readDouble(),
        Date(parcel.readLong()),
        parcel.readByte() != 0.toByte(),
        RecurringExpenseType.values()[parcel.readInt()]
    )

    constructor(title: String,
                originalAmount: Double,
                recurringDate: Date,
                type: RecurringExpenseType) : this(null, title, originalAmount, recurringDate, false, type)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(title)
        parcel.writeDouble(originalAmount)
        parcel.writeLong(recurringDate.time)
        parcel.writeByte(if (modified) 1 else 0)
        parcel.writeInt(type.ordinal)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RecurringExpense> {
        override fun createFromParcel(parcel: Parcel): RecurringExpense {
            return RecurringExpense(parcel)
        }

        override fun newArray(size: Int): Array<RecurringExpense?> {
            return arrayOfNulls(size)
        }
    }
}