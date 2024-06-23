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
package com.benoitletondor.easybudgetapp.view.monthlyreport.subviews

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.getMonthTitle
import com.benoitletondor.easybudgetapp.view.monthlyreport.MonthlyReportSelectedPosition
import java.util.Locale

@SuppressLint("PrivateResource")
@Composable
fun MonthsHeader(
    selectedPosition: MonthlyReportSelectedPosition,
    onPreviousMonthClicked: () -> Unit,
    onNextMonthClicked: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPreviousMonthClicked,
            enabled = !selectedPosition.first,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(R.color.monthly_report_month_switch_button),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_navigate_before_24),
                contentDescription = stringResource(androidx.compose.material3.R.string.m3c_date_picker_switch_to_previous_month),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = selectedPosition.month.getMonthTitle(context).uppercase(Locale.getDefault()),
            fontSize = 21.sp,
            color = colorResource(R.color.primary_text),
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNextMonthClicked,
            enabled = !selectedPosition.last,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorResource(R.color.monthly_report_month_switch_button),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_navigate_next_24),
                contentDescription = stringResource(androidx.compose.material3.R.string.m3c_date_picker_switch_to_next_month),
            )
        }
    }
}