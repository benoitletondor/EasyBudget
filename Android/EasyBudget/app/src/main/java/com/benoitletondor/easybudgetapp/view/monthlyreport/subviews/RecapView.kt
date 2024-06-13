package com.benoitletondor.easybudgetapp.view.monthlyreport.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Currency

@Composable
fun RecapView(
    userCurrencyStateFlow: StateFlow<Currency>,
    expensesAmount: Double,
    revenuesAmount: Double,
) {
    val currency by userCurrencyStateFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ){
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.revenues_total),
                    color = colorResource(R.color.monthly_report_categories_title),
                    fontSize = 18.sp,
                )

                Text(
                    text = CurrencyHelper.getFormattedCurrencyString(currency, revenuesAmount),
                    color = colorResource(R.color.monthly_report_categories_value),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.expenses_total),
                    color = colorResource(R.color.monthly_report_categories_title),
                    fontSize = 18.sp,
                )

                Text(
                    text = CurrencyHelper.getFormattedCurrencyString(currency, expensesAmount),
                    color = colorResource(R.color.monthly_report_categories_value),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.balance),
                color = colorResource(R.color.monthly_report_categories_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            val balance = revenuesAmount - expensesAmount

            Text(
                text = CurrencyHelper.getFormattedCurrencyString(currency, balance),
                color = colorResource(if (balance >= 0) R.color.budget_green else R.color.budget_red),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}