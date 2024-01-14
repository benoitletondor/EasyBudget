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

package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import java.security.SecureRandom
import java.time.LocalDate

class ExpenseEntity() : RealmObject {
    @PrimaryKey
    var _id: Long = SecureRandom().nextLong()
    var title: String = ""
    var amount: Long = 0
    @Index
    var date: Long = 0
    @Index
    var checked: Boolean = false
    @Index
    var accountId: String = ""
    @Index
    var accountSecret: String = ""

    constructor(
        id: Long?,
        title: String,
        amount: Long,
        date: Long,
        checked: Boolean,
        account: Account,
    ) : this() {
        this._id = id ?: SecureRandom().nextLong()
        this.title = title
        this.amount = amount
        this.date = date
        this.checked = checked
        this.accountId = account.id
        this.accountSecret = account.secret
    }

    fun toExpense(associatedRecurringExpense: RecurringExpense?) = Expense(
        _id,
        title,
        amount.getRealValueFromDB(),
        LocalDate.ofEpochDay(date),
        checked,
        associatedRecurringExpense?.let { AssociatedRecurringExpense(
            recurringExpense = it,
            originalDate = it.recurringDate,
        ) },
    )

    companion object {
        fun fromExpense(expense: Expense, account: Account): ExpenseEntity {
            return ExpenseEntity(
                id = expense.id,
                title = expense.title,
                amount = expense.amount.getDBValue(),
                date = expense.date.toEpochDay(),
                checked = expense.checked,
                account = account,
            )
        }
    }
}