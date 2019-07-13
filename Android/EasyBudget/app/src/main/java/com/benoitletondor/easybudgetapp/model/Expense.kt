/*
 *   Copyright 2019 Benoit LETONDOR
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