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
package com.benoitletondor.easybudgetapp.view.recurringexpenseadd

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.compose.components.ExpenseEditTextField
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.serialization.SerializedExpense
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.sanitizeFromUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.helper.stringRepresentation
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Serializable
data class RecurringExpenseAddDestination(val dateEpochDay: Long) {
    constructor(
        date: LocalDate,
    ) : this(date.toEpochDay())
}

@Serializable
data class RecurringExpenseEditDestination(val dateEpochDay: Long, val editedExpense: SerializedExpense) {
    constructor(
        date: LocalDate,
        editedExpense: Expense,
    ) : this(date.toEpochDay(), SerializedExpense(editedExpense))
}

@Composable
fun RecurringExpenseEditView(
    viewModel: RecurringExpenseEditViewModel,
    navigateUp: () -> Unit,
    finish: () -> Unit,
) {
    RecurringExpenseEditView(
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        userCurrencyFlow = viewModel.userCurrencyFlow,
        navigateUp = navigateUp,
        finish = finish,
        onTitleUpdate = viewModel::onTitleChanged,
        onAmountUpdate = viewModel::onAmountChanged,
        onSaveButtonClicked = viewModel::onSave,
        onIsRevenueChanged = viewModel::onExpenseRevenueValueChanged,
        onDateClicked = viewModel::onDateClicked,
        onDateSelected = viewModel::onDateSelected,
        onAddExpenseBeforeInitDateConfirmed = viewModel::onAddExpenseBeforeInitDateConfirmed,
        onAddExpenseBeforeInitDateCancelled = viewModel::onAddExpenseBeforeInitDateCancelled,
        onEditRecurringIntervalClicked = viewModel::onEditRecurringIntervalClicked,
        onRecurringIntervalSelected = viewModel::onRecurringExpenseTypeChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpenseEditView(
    stateFlow: StateFlow<RecurringExpenseEditViewModel.State>,
    eventFlow: Flow<RecurringExpenseEditViewModel.Event>,
    userCurrencyFlow: StateFlow<Currency>,
    navigateUp: () -> Unit,
    finish: () -> Unit,
    onTitleUpdate: (String) -> Unit,
    onAmountUpdate: (String) -> Unit,
    onSaveButtonClicked: () -> Unit,
    onIsRevenueChanged: (Boolean) -> Unit,
    onDateClicked: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    onAddExpenseBeforeInitDateConfirmed: () -> Unit,
    onAddExpenseBeforeInitDateCancelled: () -> Unit,
    onEditRecurringIntervalClicked: () -> Unit,
    onRecurringIntervalSelected: (RecurringExpenseType) -> Unit,
) {
    val context = LocalContext.current
    var showDatePickerWithDate by remember { mutableStateOf<LocalDate?>(null) }
    var amountValueError: String? by remember { mutableStateOf(null) }
    var titleValueError: String? by remember { mutableStateOf(null) }
    var shouldShowRecurringIntervalPicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                RecurringExpenseEditViewModel.Event.ExpenseAddBeforeInitDateError -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_add_before_init_date_dialog_title)
                    .setMessage(R.string.expense_add_before_init_date_dialog_description)
                    .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                        onAddExpenseBeforeInitDateConfirmed()
                    }
                    .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                        onAddExpenseBeforeInitDateCancelled()
                    }
                    .show()
                RecurringExpenseEditViewModel.Event.Finish -> finish()
                RecurringExpenseEditViewModel.Event.UnableToLoadDB -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_edit_unable_to_load_db_error_title)
                    .setMessage(R.string.expense_edit_unable_to_load_db_error_message)
                    .setPositiveButton(R.string.expense_edit_unable_to_load_db_error_cta) { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
                is RecurringExpenseEditViewModel.Event.ErrorPersistingExpense -> MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.expense_edit_error_saving_title)
                    .setMessage(R.string.expense_edit_error_saving_message)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                RecurringExpenseEditViewModel.Event.EmptyTitleError -> titleValueError = context.getString(R.string.no_description_error)
                is RecurringExpenseEditViewModel.Event.ShowDatePicker -> showDatePickerWithDate = event.date
                RecurringExpenseEditViewModel.Event.EmptyAmountError -> amountValueError = context.getString(
                    R.string.no_amount_error)
                RecurringExpenseEditViewModel.Event.ShowRecurringIntervalPicker -> shouldShowRecurringIntervalPicker = true
            }
        }
    }

    val state by stateFlow.collectAsState()

    BackHandler(enabled = state.isSaving) {
        /* No-op to disable back press while saving */
    }

    AppWithTopAppBarScaffold(
        title = stringResource(if (state.isEditing) {
            if (state.isRevenue) { R.string.title_activity_recurring_income_edit } else { R.string.title_activity_recurring_expense_edit }
        } else {
            if (state.isRevenue) { R.string.title_activity_recurring_income_add } else { R.string.title_activity_recurring_expense_add }
        }),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            val titleFocusRequester = remember { FocusRequester() }
            val amountFocusRequester = remember { FocusRequester() }
            LaunchedEffect(key1 = "focusRequester") {
                titleFocusRequester.requestFocus()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = colorResource(R.color.action_bar_background))
                            .padding(horizontal = 26.dp)
                            .padding(top = 10.dp, bottom = 30.dp),
                    ) {
                        var descriptionTextFieldValue by remember { mutableStateOf(
                            TextFieldValue(
                                text = state.expense.title,
                                selection = TextRange(index = state.expense.title.length),
                            )
                        ) }

                        ExpenseEditTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester),
                            value = descriptionTextFieldValue,
                            onValueChange = {
                                descriptionTextFieldValue = it
                                titleValueError = null
                                onTitleUpdate(it.text)
                            },
                            isError = titleValueError != null,
                            label = if (titleValueError != null ) "${stringResource(R.string.description)}: $titleValueError" else stringResource(
                                R.string.description),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    titleFocusRequester.freeFocus()
                                    amountFocusRequester.requestFocus()
                                },
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences,
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val currency by userCurrencyFlow.collectAsState()

                            var currentAmountTextFieldValue by remember { mutableStateOf(
                                TextFieldValue(
                                    text = if (state.expense.amount == 0.0) "" else CurrencyHelper.getFormattedAmountValue(
                                        abs(state.expense.amount)
                                    ),
                                    selection = TextRange(index = if (state.expense.amount == 0.0) 0 else CurrencyHelper.getFormattedAmountValue(
                                        abs(state.expense.amount)
                                    ).length),
                                )
                            ) }

                            ExpenseEditTextField(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .focusRequester(amountFocusRequester),
                                value = currentAmountTextFieldValue,
                                onValueChange = { newValue ->
                                    val newText = newValue.text.sanitizeFromUnsupportedInputForDecimals(supportsNegativeValue = false)

                                    currentAmountTextFieldValue = TextFieldValue(
                                        text = newText,
                                        selection = newValue.selection,
                                    )

                                    amountValueError = null
                                    onAmountUpdate(newText)
                                },
                                isError = amountValueError != null,
                                label = if (amountValueError != null) "${stringResource(R.string.amount, currency.symbol)}: $amountValueError" else stringResource(
                                    R.string.amount, currency.symbol),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                )
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .padding(top = 5.dp),
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(R.string.recurring_expense_interval),
                                    color = colorResource(R.color.expense_edit_title_text_color_dark),
                                    fontSize = 15.sp,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onEditRecurringIntervalClicked)
                                        .padding(top = 2.dp),
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = state.recurringExpenseType.stringRepresentation(context),
                                        color = colorResource(R.color.expense_edit_field_accent_color_dark),
                                        fontSize = 17.sp,
                                    )

                                    Spacer(modifier = Modifier.height(5.dp))

                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = colorResource(R.color.expense_edit_field_accent_color_dark),
                                        thickness = 1.dp,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.End)
                            .offset(y = (-30).dp, x = (-26).dp),
                        onClick = onSaveButtonClicked,
                        containerColor = colorResource(R.color.secondary),
                        contentColor = colorResource(R.color.white),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_white_24dp),
                            contentDescription = stringResource(R.string.fab_add_expense),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.type),
                                color = colorResource(R.color.expense_edit_title_text_color),
                                fontSize = 14.sp,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Switch(
                                    checked = state.isRevenue,
                                    onCheckedChange = onIsRevenueChanged,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = colorResource(R.color.add_expense_expense_thumb_background_color),
                                        checkedThumbColor = colorResource(R.color.budget_green),
                                        uncheckedThumbColor = colorResource(R.color.budget_red),
                                        uncheckedTrackColor = colorResource(R.color.add_expense_expense_thumb_background_color),
                                        uncheckedBorderColor = Color.Transparent,
                                        checkedBorderColor = Color.Transparent,
                                    ),
                                    thumbContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.width(5.dp))

                                Text(
                                    text = stringResource(if(state.isRevenue) R.string.income else R.string.payment),
                                    color = colorResource(if(state.isRevenue) R.color.budget_green else R.color.budget_red),
                                    fontSize = 14.sp,
                                )
                            }


                        }

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.first_occurence),
                                color = colorResource(R.color.expense_edit_title_text_color),
                                fontSize = 14.sp,
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            val dateFormatter = remember {
                                DateTimeFormatter.ofPattern(context.getString(R.string.add_expense_date_format), Locale.getDefault())
                            }
                            val dateString = remember(state.expense.date) {
                                dateFormatter.format(state.expense.date)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onDateClicked)
                                    .padding(top = 3.dp),
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = dateString,
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorResource(R.color.expense_edit_field_accent_color),
                                    thickness = 1.dp,
                                )
                            }
                        }
                    }
                }
            }

            val datePickerDate = showDatePickerWithDate
            if (datePickerDate != null) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = Date.from(datePickerDate.atStartOfDay().atZone(
                        ZoneId.of("UTC")).toInstant()).time
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePickerWithDate = null },
                    confirmButton = {
                        Button(
                            modifier = Modifier.padding(end = 16.dp, bottom = 10.dp),
                            onClick = {
                                onDateSelected(datePickerState.selectedDateMillis)
                                showDatePickerWithDate = null
                            }
                        ) {
                            Text(text = stringResource(R.string.ok))
                        }
                    },
                    content = {
                        DatePicker(state = datePickerState)
                    },
                )
            }

            if (shouldShowRecurringIntervalPicker) {
                RecurringIntervalPickerDialog(
                    onDismissRequest = { shouldShowRecurringIntervalPicker = false },
                    onIntervalSelected = { interval ->
                        onRecurringIntervalSelected(interval)
                        shouldShowRecurringIntervalPicker = false
                    },
                )
            }

            if (state.isSaving) {
                Dialog(onDismissRequest = { /* No-op */ }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(R.color.window_background),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(if (state.isRevenue) R.string.recurring_income_add_loading_message else R.string.recurring_expense_add_loading_message),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun RecurringIntervalPickerDialog(
    onDismissRequest: () -> Unit,
    onIntervalSelected: (RecurringExpenseType) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.window_background),
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 10.dp),
                    text = stringResource(R.string.recurring_expense_interval),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )

                RecurringExpenseType.entries.forEach { recurringExpenseType ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = colorResource(R.color.window_background))
                            .clickable { onIntervalSelected(recurringExpenseType) }
                            .padding(horizontal = 26.dp, vertical = 16.dp),
                        text = recurringExpenseType.stringRepresentation(LocalContext.current),
                    )
                }
            }
        }
    }
}