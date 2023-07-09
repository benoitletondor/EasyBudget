package com.benoitletondor.easybudgetapp.db.onlineimpl.entity

import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.helper.getDBValue
import com.benoitletondor.easybudgetapp.helper.getRealValueFromDB
import com.benoitletondor.easybudgetapp.model.AssociatedRecurringExpense
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpense
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.security.SecureRandom
import java.time.LocalDate

class ExpenseEntity() : RealmObject {
    @PrimaryKey
    var _id: Long = SecureRandom().nextLong()
    var title: String = ""
    var amount: Long = 0
    var date: Long = 0
    var checked: Boolean = false
    var accountId: String = ""
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