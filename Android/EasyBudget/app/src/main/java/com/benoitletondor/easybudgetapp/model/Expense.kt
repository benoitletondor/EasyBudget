/*
 *   Copyright 2024 Benoit LETONDOR
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
import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class Expense(val id: Long?,
                   val title: String,
                   val amount: Double,
                   val date: LocalDate,
                   val checked: Boolean,
                   val associatedRecurringExpense: AssociatedRecurringExpense?) : Parcelable {

    constructor(title: String,
                amount: Double,
                date: LocalDate,
                checked: Boolean) : this(null, title, amount, date, checked, null)

    constructor(id: Long,
                title: String,
                amount: Double,
                date: LocalDate,
                checked: Boolean) : this(id, title, amount, date, checked, null)

    constructor(title: String,
                amount: Double,
                date: LocalDate,
                checked: Boolean,
                associatedRecurringExpense: AssociatedRecurringExpense) : this(null, title, amount, date, checked, associatedRecurringExpense)

    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()!!,
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readInt() == 1,
        parcel.readParcelable(AssociatedRecurringExpense::class.java.classLoader)
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
        parcel.writeLong(date.toEpochDay())
        parcel.writeInt(if( checked ) { 1 } else { 0 })
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