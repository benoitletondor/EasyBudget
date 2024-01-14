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

package com.benoitletondor.easybudgetapp.view.main.account.calendar.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarHeaderView(
    month: YearMonth,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onMonthChange: (YearMonth) -> Unit,
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "<",
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(CircleShape)
                .clickable(enabled = canGoBack) {
                    onMonthChange(month.minusMonths(1))
                }
                .padding(vertical = 8.dp, horizontal = 16.dp),
            color = colorResource(id = if(canGoBack) R.color.calendar_month_button_color else R.color.calendar_month_button_color_disabled),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = month.format(monthFormatter).uppercase(),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = colorResource(id = R.color.calendar_header_month_color),
            modifier = Modifier.weight(1f),
        )

        Text(
            text = ">",
            modifier = Modifier
                .padding(end = 8.dp)
                .clip(CircleShape)
                .clickable(enabled = canGoForward) {
                    onMonthChange(month.plusMonths(1))
                }
                .padding(vertical = 8.dp, horizontal = 16.dp),
            color = colorResource(id = if(canGoForward) R.color.calendar_month_button_color else R.color.calendar_month_button_color_disabled),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}