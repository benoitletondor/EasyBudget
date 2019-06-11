package com.benoitletondor.easybudgetapp.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Expense(val id: Long?,
                   val title: String,
                   val amount: Double,
                   val date: Date,
                   val associatedRecurringExpense: RecurringExpense?) : Parcelable {

    constructor(title: String,
                amount: Double,
                date: Date) : this(null, title, amount, date, null)

    constructor(id: Long,
                title: String,
                amount: Double,
                date: Date) : this(id, title, amount, date, null)

    constructor(title: String,
                amount: Double,
                date: Date,
                associatedRecurringExpense: RecurringExpense) : this(null, title, amount, date, associatedRecurringExpense)

    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()!!,
        parcel.readDouble(),
        Date(parcel.readLong()),
        parcel.readParcelable(RecurringExpense::class.java.classLoader)
    )

    init {
        if( title.isEmpty() ) {
            throw IllegalArgumentException("title is empty")
        }

        if( amount == 0.0 ) {
            throw IllegalArgumentException("amount should be != 0")
        }
    }

    fun isRevenue() = amount < 0

    fun isRecurring() = associatedRecurringExpense != null

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(title)
        parcel.writeDouble(amount)
        parcel.writeLong(date.time)
        parcel.writeParcelable(associatedRecurringExpense, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Expense> {
        override fun createFromParcel(parcel: Parcel): Expense {
            return Expense(parcel)
        }

        override fun newArray(size: Int): Array<Expense?> {
            return arrayOfNulls(size)
        }
    }

}