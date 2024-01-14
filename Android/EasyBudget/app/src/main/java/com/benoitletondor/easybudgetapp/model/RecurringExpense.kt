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
data class RecurringExpense(val id: Long?,
                            val title: String,
                            val amount: Double,
                            val recurringDate: LocalDate,
                            val modified: Boolean,
                            val type: RecurringExpenseType) : Parcelable {
    
    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()!!,
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readByte() != 0.toByte(),
        RecurringExpenseType.entries[parcel.readInt()]
    )

    constructor(title: String,
                originalAmount: Double,
                recurringDate: LocalDate,
                type: RecurringExpenseType) : this(null, title, originalAmount, recurringDate, false, type)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(title)
        parcel.writeDouble(amount)
        parcel.writeLong(recurringDate.toEpochDay())
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