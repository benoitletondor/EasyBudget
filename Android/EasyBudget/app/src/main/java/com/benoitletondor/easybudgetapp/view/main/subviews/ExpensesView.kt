/*
 *   Copyright 2025 Benoit Letondor
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
package com.benoitletondor.easybudgetapp.view.main.subviews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.toFormattedString
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun ColumnScope.ExpensesView(
    dayDataFlow: StateFlow<MainViewModel.SelectedDateExpensesData>,
    lowMoneyAmountWarningFlow: StateFlow<Int>,
    userCurrencyFlow: StateFlow<Currency>,
    showExpensesCheckBoxFlow: StateFlow<Boolean>,
    onExpenseCheckedChange: (Expense, Boolean) -> Unit,
    onExpensePressed: (Expense) -> Unit,
    onExpenseLongPressed: (Expense) -> Unit,
) {
    val dataForDay by dayDataFlow.collectAsState()
    val userCurrency by userCurrencyFlow.collectAsState()

    when(val dayData = dataForDay) {
        is MainViewModel.SelectedDateExpensesData.DataAvailable -> {
            BalanceView(
                date = dayData.date,
                balance = dayData.balance,
                checkedBalance = dayData.checkedBalance,
                userCurrency = userCurrency,
                lowMoneyAmountWarningFlow = lowMoneyAmountWarningFlow,
            )

            ExpensesList(
                expenses = dayData.expenses,
                userCurrency = userCurrency,
                showExpensesCheckBoxFlow = showExpensesCheckBoxFlow,
                onExpenseCheckedChange = onExpenseCheckedChange,
                onExpensePressed = onExpensePressed,
                onExpenseLongPressed = onExpenseLongPressed,
            )
        }
        MainViewModel.SelectedDateExpensesData.NoDataAvailable -> {
            LoadingView()
        }
    }
}

@Composable
private fun BalanceView(
    date: LocalDate,
    balance: Double,
    checkedBalance: Double?,
    userCurrency: Currency,
    lowMoneyAmountWarningFlow: StateFlow<Int>,
) {
    val context = LocalContext.current
    val balanceDateFormatter = remember(key1 = Locale.getDefault()) {
        DateTimeFormatter.ofPattern(context.getString(R.string.account_balance_date_format), Locale.getDefault())
    }
    val lowMoneyAmountWarning by lowMoneyAmountWarningFlow.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(R.color.budget_line_background_color))
            .padding(horizontal = 15.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val formattedDate = remember(key1 = date, key2 = balanceDateFormatter) {
            context.getString(R.string.account_balance_format, balanceDateFormatter.format(date)).let {
                // FIXME it's ugly!!
                if (it.endsWith(".:")) {
                    return@let it.substring(0, it.length - 2) + ":" // Remove . at the end of the month (ex: nov.: -> nov:)
                } else if (it.endsWith(". :")) {
                    return@let it.substring(0, it.length - 3) + " :" // Remove . at the end of the month (ex: nov. : -> nov :)
                } else {
                    return@let it
                }
            }
        }

        Text(
            text = formattedDate,
            fontSize = 14.sp,
            color = colorResource(R.color.primary_text),
        )

        Spacer(modifier = Modifier.width(4.dp))

        val checkedBalanceString = remember(key1 = balance, key2 = checkedBalance, key3 = userCurrency) {
            if (checkedBalance != null) {
                context.getString(
                    R.string.account_balance_checked_format,
                    CurrencyHelper.getFormattedCurrencyString(userCurrency, balance),
                    CurrencyHelper.getFormattedCurrencyString(userCurrency, checkedBalance),
                )
            } else {
                CurrencyHelper.getFormattedCurrencyString(userCurrency, balance)
            }
        }

        Text(
            text = checkedBalanceString,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = when {
                balance <= 0 -> colorResource(R.color.budget_red)
                balance < lowMoneyAmountWarning -> colorResource(R.color.budget_orange)
                else -> colorResource(R.color.budget_green)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ExpensesList(
    expenses: List<Expense>,
    userCurrency: Currency,
    showExpensesCheckBoxFlow: StateFlow<Boolean>,
    onExpenseCheckedChange: (Expense, Boolean) -> Unit,
    onExpensePressed: (Expense) -> Unit,
    onExpenseLongPressed: (Expense) -> Unit,
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.alpha(0.6f),
                painter = painterResource(R.drawable.ic_wallet),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = stringResource(R.string.no_expense_for_today),
                fontSize = 14.sp,
                color = colorResource(R.color.secondary_text),
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            items(
                count = expenses.size,
            ) { index ->
                val expense = expenses[index]

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onExpensePressed(expense) },
                                onLongClick = { onExpenseLongPressed(expense) },
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val context = LocalContext.current

                        Image(
                            modifier = Modifier.size(30.dp),
                            painter = painterResource(if (expense.isRevenue()) R.drawable.ic_label_green else R.drawable.ic_label_red),
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = expense.title,
                                fontSize = 14.sp,
                                maxLines = 2,
                                color = colorResource(R.color.primary_text),
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = CurrencyHelper.getFormattedCurrencyString(userCurrency, -expense.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = colorResource(if (expense.isRevenue()) R.color.budget_green else R.color.budget_red),
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (expense.isRecurring()) {
                            Column(
                                modifier = Modifier.width(60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_autorenew_grey_26dp),
                                    contentDescription = null,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false,
                                        ),
                                    ),
                                    text = expense.associatedRecurringExpense!!.recurringExpense.type.toFormattedString(context),
                                    fontSize = 9.sp,
                                    color = colorResource(R.color.secondary_text),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        val showCheckBox by showExpensesCheckBoxFlow.collectAsState()
                        if (showCheckBox) {
                            Spacer(modifier = Modifier.width(10.dp))

                            // Remove padding from the checkbox
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Checkbox(
                                    checked = expense.checked,
                                    onCheckedChange = { checked ->
                                        onExpenseCheckedChange(expense, checked)
                                    },
                                )
                            }
                        }
                    }

                    if (index < expenses.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 70.dp),
                            color = colorResource(R.color.divider),
                            thickness = 1.dp,
                        )
                    } else {
                        // Add inner padding for the FAB
                        Spacer(modifier = Modifier.height(80.dp))
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    }
                }


            }
        }
    }
}