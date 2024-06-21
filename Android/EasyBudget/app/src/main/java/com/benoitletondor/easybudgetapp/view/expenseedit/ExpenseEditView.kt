package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.SerializedExpense
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
        isEditingStateFlow = viewModel.isEditingStateFlow,
        expenseStateFlow = viewModel.expenseStateFlow,
        eventFlow = viewModel.eventFlow,
        navigateUp = navigateUp,
        finish = finish,
    )
}

@Composable
private fun ExpenseEditView(
    isEditingStateFlow: StateFlow<Boolean>,
    expenseStateFlow: StateFlow<Expense>,
    eventFlow: Flow<ExpenseEditViewModel.Event>,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                ExpenseEditViewModel.Event.ExpenseAddBeforeInitDateError -> TODO()
                ExpenseEditViewModel.Event.Finish -> finish()
                ExpenseEditViewModel.Event.UnableToLoadDB -> TODO()
            }
        }
    }

    val isEditing by isEditingStateFlow.collectAsState()
    val expense by expenseStateFlow.collectAsState()

    AppWithTopAppBarScaffold(
        title = stringResource(if (isEditing) {
            if (expense.isRevenue()) { R.string.title_activity_edit_income } else { R.string.title_activity_edit_expense }
        } else {
            if (expense.isRevenue()) { R.string.title_activity_add_income } else { R.string.title_activity_add_expense }
        }),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // TODO
            }
        },
    )
}