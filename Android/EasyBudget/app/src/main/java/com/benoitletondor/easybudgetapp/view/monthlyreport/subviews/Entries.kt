package com.benoitletondor.easybudgetapp.view.monthlyreport.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.model.Expense
import kotlinx.coroutines.flow.StateFlow
import java.util.Currency

@Composable
fun EntriesView(
    userCurrencyStateFlow: StateFlow<Currency>,
    expenses: List<Expense>,
    revenues: List<Expense>,
) {
    val currency by userCurrencyStateFlow.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(expenses.size + revenues.size + 2) { index ->
            when(index) {
                0 -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = colorResource(R.color.budget_green))
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        text = stringResource(R.string.revenues),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                }
                revenues.size + 1 -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = colorResource(R.color.budget_red))
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        text = stringResource(R.string.expenses),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                }
                else -> {
                    when(index) {
                        in 1 until revenues.size + 1 -> {
                            val revenue = revenues.getOrNull(index - 1)
                            if (revenue != null) {
                                Entry(
                                    currency = currency,
                                    expense = revenue,
                                    includeDivider = index < revenues.size,
                                )
                            }
                        }
                        in revenues.size + 1 until revenues.size + expenses.size + 1 -> {
                            val expense = expenses.getOrNull(index - revenues.size - 1)
                            if (expense != null) {
                                Entry(
                                    currency = currency,
                                    expense = expense,
                                    includeDivider = index < revenues.size + expenses.size,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}