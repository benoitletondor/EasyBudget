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

package com.benoitletondor.easybudgetapp.db.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import java.util.*

@Entity(tableName = "monthlyexpense")
class RecurringExpenseEntity(@PrimaryKey
                             @ColumnInfo(name = "_expense_id")
                             val id: Long?,
                             @ColumnInfo(name = "title")
                             val title: String,
                             @ColumnInfo(name = "amount")
                             val originalAmount: Long,
                             @ColumnInfo(name = "recurringDate")
                             val recurringDate: Date,
                             @ColumnInfo(name = "modified")
                             val modified: Boolean, // Not implemented yet
                             @ColumnInfo(name = "type")
                             val type: String) {

    fun toRecurringExpense() = RecurringExpense(
        id,
        title,
        originalAmount / 100.0,
        recurringDate,
        modified,
        RecurringExpenseType.valueOf(type)
    )
}