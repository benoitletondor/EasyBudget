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
