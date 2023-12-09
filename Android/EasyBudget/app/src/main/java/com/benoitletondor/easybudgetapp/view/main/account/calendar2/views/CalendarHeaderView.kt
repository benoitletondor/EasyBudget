package com.benoitletondor.easybudgetapp.view.main.account.calendar2.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
        if (canGoBack) {
            Button(
                onClick = {
                    onMonthChange(month.minusMonths(1))
                }
            ) {
                Text("<")
            }
        }

        Text(
            text = month.format(monthFormatter).uppercase(),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )

        if (canGoForward) {
            Button(
                onClick = {
                    onMonthChange(month.plusMonths(1))
                }
            ) {
                Text(">")
            }
        }
    }
}