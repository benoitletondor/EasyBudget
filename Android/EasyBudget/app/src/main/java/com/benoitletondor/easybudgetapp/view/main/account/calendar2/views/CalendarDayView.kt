package com.benoitletondor.easybudgetapp.view.main.account.calendar2.views

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R

@Composable
fun InCalendarWithBalanceDayView(
    dayOfMonth: Int,
    balanceToDisplay: Double,
    displayUncheckedStyle: Boolean,
    selected: Boolean,
    today: Boolean,
) {
    InCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = balanceToDisplay,
        displayUncheckedStyle = displayUncheckedStyle,
        selected = selected,
        today = today,
    )
}

@Composable
fun InCalendarEmptyDayView(
    dayOfMonth: Int,
    selected: Boolean,
    today: Boolean,
) {
    InCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = null,
        displayUncheckedStyle = false,
        selected = selected,
        today = today,
    )
}

@Composable
fun OffCalendarWithBalanceDayView(
    dayOfMonth: Int,
    balanceToDisplay: Double,
    displayUncheckedStyle: Boolean,
    today: Boolean,
) {
    OffCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = balanceToDisplay,
        displayUncheckedStyle = displayUncheckedStyle,
        today = today,
    )
}

@Composable
fun OffCalendarEmptyDayView(
    dayOfMonth: Int,
    today: Boolean,
) {
    OffCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = null,
        displayUncheckedStyle = false,
        today = today,
    )
}

@Composable
private fun InCalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    displayUncheckedStyle: Boolean,
    selected: Boolean,
    today: Boolean,
) {
    CalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = maybeBalanceToDisplay,
        dayOfMonthColor = colorResource(id = getDayOfMonthColor(
            maybeBalance = maybeBalanceToDisplay,
            isInMonth = true,
        )),
        dayOfMonthFontStyle = if (displayUncheckedStyle) FontStyle.Italic else FontStyle.Normal,
        balanceColor = colorResource(id = R.color.primary_text),
        selected = selected,
        today = today,
    )
}

@Composable
private fun OffCalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    displayUncheckedStyle: Boolean,
    today: Boolean,
) {
    CalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = maybeBalanceToDisplay,
        dayOfMonthColor = colorResource(id = getDayOfMonthColor(
            maybeBalance = maybeBalanceToDisplay,
            isInMonth = false,
        )),
        dayOfMonthFontStyle = if (displayUncheckedStyle) FontStyle.Italic else FontStyle.Normal,
        balanceColor = colorResource(id = R.color.divider),
        selected = false,
        today = today,
    )
}

@ColorRes
private fun getDayOfMonthColor(
    maybeBalance: Double?,
    isInMonth: Boolean,
): Int {
    val lowMoneyWarningAmount = 500 // FIXME parameters.getLowMoneyWarningAmount()

    return when{
        maybeBalance == null -> if (isInMonth) { R.color.primary_text } else { R.color.divider }
        -maybeBalance <= 0 -> R.color.budget_red
        -maybeBalance < lowMoneyWarningAmount -> R.color.budget_orange
        else -> R.color.budget_green
    }
}

@Composable
private fun CalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    dayOfMonthColor: Color,
    dayOfMonthFontStyle: FontStyle,
    balanceColor: Color,
    selected: Boolean,
    today: Boolean,
) {
   Column(
       modifier = Modifier
           .padding(2.dp)
           .fillMaxWidth()
           .aspectRatio(1f)
           .background(
               color = if (selected) {
                   colorResource(id = R.color.calendar_cell_selected)
               } else {
                   Color.Transparent
               }
           )
           .clip(shape = CircleShape)
           .border(
               width = 2.dp,
               color = if (today) MaterialTheme.colorScheme.primary else Color.Transparent,
               shape = CircleShape
           )
           .padding(7.dp)
   ) {
        Text(
            text = dayOfMonth.toString(),
            color = dayOfMonthColor,
            fontStyle = dayOfMonthFontStyle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = maybeBalanceToDisplay?.toString() ?: "",
            color = balanceColor,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
   }
}