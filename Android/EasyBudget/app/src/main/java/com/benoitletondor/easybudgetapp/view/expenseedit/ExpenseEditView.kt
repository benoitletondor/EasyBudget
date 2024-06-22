package com.benoitletondor.easybudgetapp.view.expenseedit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import com.benoitletondor.easybudgetapp.helper.SerializedExpense
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.sanitizeFromUnsupportedInputForDecimals
import com.benoitletondor.easybudgetapp.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Currency

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
        stateFlow = viewModel.stateFlow,
        eventFlow = viewModel.eventFlow,
        userCurrencyFlow = viewModel.userCurrencyFlow,
        navigateUp = navigateUp,
        finish = finish,
        onTitleUpdate = viewModel::onTitleChanged,
        onAmountUpdate = viewModel::onAmountChanged,
        onSaveButtonClicked = viewModel::onSave,
        onIsRevenueChanged = viewModel::onExpenseRevenueValueChanged,
    )
}

@Composable
private fun ExpenseEditView(
    stateFlow: StateFlow<ExpenseEditViewModel.State>,
    eventFlow: Flow<ExpenseEditViewModel.Event>,
    userCurrencyFlow: StateFlow<Currency>,
    navigateUp: () -> Unit,
    finish: () -> Unit,
    onTitleUpdate: (String) -> Unit,
    onAmountUpdate: (String) -> Unit,
    onSaveButtonClicked: () -> Unit,
    onIsRevenueChanged: (Boolean) -> Unit,
) {
    LaunchedEffect(key1 = "eventsListener") {
        launchCollect(eventFlow) { event ->
            when (event) {
                ExpenseEditViewModel.Event.ExpenseAddBeforeInitDateError -> TODO()
                ExpenseEditViewModel.Event.Finish -> finish()
                ExpenseEditViewModel.Event.UnableToLoadDB -> TODO()
                is ExpenseEditViewModel.Event.ErrorPersistingExpense -> TODO()
                ExpenseEditViewModel.Event.EmptyTitleError -> TODO()
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = "focusRequester") {
        focusRequester.requestFocus()
    }

    val state by stateFlow.collectAsState()

    AppWithTopAppBarScaffold(
        title = stringResource(if (state.isEditing) {
            if (state.isRevenue) { R.string.title_activity_edit_income } else { R.string.title_activity_edit_expense }
        } else {
            if (state.isRevenue) { R.string.title_activity_add_income } else { R.string.title_activity_add_expense }
        }),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorResource(R.color.action_bar_background))
                        .padding(horizontal = 26.dp, vertical = 10.dp)
                ) {
                    var descriptionTextFieldValue by remember { mutableStateOf(
                        TextFieldValue(
                            text = state.expense.title,
                            selection = TextRange(index = state.expense.title.length),
                        )
                    ) }

                    InputTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = descriptionTextFieldValue,
                        onValueChange = {
                            descriptionTextFieldValue = it
                            onTitleUpdate(it.text)
                        },
                        label = stringResource(R.string.description),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val currency by userCurrencyFlow.collectAsState()

                    var currentAmountTextFieldValue by remember { mutableStateOf(
                        TextFieldValue(
                            text = if (state.expense.amount == 0.0) "" else state.expense.amount.toString(),
                            selection = TextRange(index = if (state.expense.amount == 0.0) 0 else state.expense.amount.toString().length),
                        )
                    ) }

                    InputTextField(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        value = currentAmountTextFieldValue,
                        onValueChange = { newValue ->
                            val newText = newValue.text.sanitizeFromUnsupportedInputForDecimals()

                            currentAmountTextFieldValue = TextFieldValue(
                                text = newText,
                                selection = newValue.selection,
                            )

                            onAmountUpdate(newText)
                        },
                        label = stringResource(R.string.amount, currency.symbol),
                    )

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
                                    checkedTrackColor = Color(0xFFDDDDDD),
                                    checkedThumbColor = colorResource(R.color.budget_green),
                                    uncheckedThumbColor = colorResource(R.color.budget_red),
                                    uncheckedTrackColor = Color(0xFFDDDDDD),
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

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {

                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 17.sp,
    ),
    label: String,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = colorResource(R.color.action_bar_background),
        unfocusedContainerColor = colorResource(R.color.action_bar_background),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedIndicatorColor = Color.White,
        unfocusedIndicatorColor = Color.White,
        selectionColors = TextSelectionColors(
            handleColor = Color.White,
            backgroundColor = Color.White.copy(alpha = 0.3f),
        )
    ),
) {
    val textColor = textStyle.color
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.White,
        backgroundColor = Color.White.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            modifier = modifier
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(Color.White),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = @Composable { innerTextField ->
                // places leading icon, text field with label and placeholder, trailing icon
                TextFieldDefaults.DecorationBox(
                    value = value.text,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = {
                        Text(
                            modifier = Modifier.padding(bottom = 2.dp),
                            text = label,
                            fontSize = 15.sp,
                        )
                    },
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    shape = shape,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = PaddingValues(top = 5.dp),
                )
            }
        )
    }
}