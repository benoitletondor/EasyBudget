package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.compose.runtime.Composable
import com.benoitletondor.easybudgetapp.helper.SerializedExpense
import com.benoitletondor.easybudgetapp.model.Expense
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ExpenseAddDestination(val dateEpochDay: Long) {
    constructor(
        date: LocalDate,
    ) : this(date.toEpochDay())
}

@Serializable
data class ExpenseEditDestination(val dateEpochDay: Long, val editedExpense: SerializedExpense) {
    constructor(
        date: LocalDate,
        editedExpense: Expense,
    ) : this(date.toEpochDay(), SerializedExpense(editedExpense))
}

@Composable
fun ExpenseEditView(
    viewModel: ExpenseEditViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    ExpenseEditView(

    )
}

@Composable
private fun ExpenseEditView(

) {

}