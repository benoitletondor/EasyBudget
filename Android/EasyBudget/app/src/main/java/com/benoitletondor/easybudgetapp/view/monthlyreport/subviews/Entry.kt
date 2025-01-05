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
package com.benoitletondor.easybudgetapp.view.monthlyreport.subviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.toFormattedString
import com.benoitletondor.easybudgetapp.model.Expense
import java.util.Currency

@Composable
fun Entry(
    currency: Currency,
    expense: Expense,
    includeDivider: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ){
            Text(
                modifier = Modifier
                    .paint(
                        painter = painterResource(R.drawable.ic_date),
                        contentScale = ContentScale.FillBounds,
                    )
                    .wrapContentHeight()
                    .padding(top = 4.dp),
                text = expense.date.dayOfMonth.toString(),
                fontSize = 14.sp,
                color = colorResource(R.color.monthly_report_date_color),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false,
                    ),
                ),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = expense.title,
                    color = colorResource(R.color.primary_text),
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = CurrencyHelper.getFormattedCurrencyString(currency, -expense.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(if (expense.isRevenue()) R.color.budget_green else R.color.budget_red),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (expense.isRecurring()) {
                Spacer(modifier = Modifier.width(16.dp))

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
                        text = expense.associatedRecurringExpense!!.recurringExpense.type.toFormattedString(
                            LocalContext.current),
                        fontSize = 9.sp,
                        color = colorResource(R.color.secondary_text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (includeDivider) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.divider),
                thickness = 1.dp,
            )
        }
    }
}