package com.benoitletondor.easybudgetapp.view.main.account.calendar.views

import androidx.annotation.ColorRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import com.benoitletondor.easybudgetapp.view.main.account.calendar.NumberFormatter
import com.benoitletondor.easybudgetapp.view.main.account.calendar.RoundedToIntNumberFormatter

@Composable
fun InCalendarWithBalanceDayView(
    dayOfMonth: Int,
    balanceToDisplay: Double,
    lowMoneyWarningAmount: Int,
    displayUncheckedStyle: Boolean,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    InCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = balanceToDisplay,
        lowMoneyWarningAmount = lowMoneyWarningAmount,
        displayUncheckedStyle = displayUncheckedStyle,
        selected = selected,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
fun InCalendarEmptyDayView(
    dayOfMonth: Int,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    InCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = null,
        lowMoneyWarningAmount = null,
        displayUncheckedStyle = false,
        selected = selected,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
fun OffCalendarWithBalanceDayView(
    dayOfMonth: Int,
    balanceToDisplay: Double,
    lowMoneyWarningAmount: Int,
    displayUncheckedStyle: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    OffCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = balanceToDisplay,
        lowMoneyWarningAmount = lowMoneyWarningAmount,
        displayUncheckedStyle = displayUncheckedStyle,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
fun OffCalendarEmptyDayView(
    dayOfMonth: Int,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    OffCalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = null,
        lowMoneyWarningAmount = null,
        displayUncheckedStyle = false,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun InCalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    lowMoneyWarningAmount: Int?,
    displayUncheckedStyle: Boolean,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    CalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = maybeBalanceToDisplay,
        dayOfMonthColor = colorResource(id = getDayOfMonthColor(
            maybeBalance = maybeBalanceToDisplay,
            lowMoneyWarningAmount = lowMoneyWarningAmount ?: 0,
            isInMonth = true,
        )),
        dayOfMonthFontStyle = if (displayUncheckedStyle) FontStyle.Italic else FontStyle.Normal,
        balanceColor = colorResource(id = R.color.primary_text),
        selected = selected,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun OffCalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    lowMoneyWarningAmount: Int?,
    displayUncheckedStyle: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    CalendarDayView(
        dayOfMonth = dayOfMonth,
        maybeBalanceToDisplay = maybeBalanceToDisplay,
        dayOfMonthColor = colorResource(id = getDayOfMonthColor(
            maybeBalance = maybeBalanceToDisplay,
            lowMoneyWarningAmount = lowMoneyWarningAmount ?: 0,
            isInMonth = false,
        )),
        dayOfMonthFontStyle = if (displayUncheckedStyle) FontStyle.Italic else FontStyle.Normal,
        balanceColor = colorResource(id = R.color.divider),
        selected = false,
        today = today,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@ColorRes
private fun getDayOfMonthColor(
    maybeBalance: Double?,
    lowMoneyWarningAmount: Int,
    isInMonth: Boolean,
): Int {
    return when{
        maybeBalance == null -> if (isInMonth) { R.color.primary_text } else { R.color.divider }
        -maybeBalance <= 0 -> R.color.budget_red
        -maybeBalance < lowMoneyWarningAmount -> R.color.budget_orange
        else -> R.color.budget_green
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayView(
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    dayOfMonthColor: Color,
    dayOfMonthFontStyle: FontStyle,
    balanceColor: Color,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape = CircleShape)
                .background(
                    color = if (selected) {
                        colorResource(id = R.color.calendar_cell_selected)
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (today) colorResource(id = R.color.calendar_today_stroke_color) else Color.Transparent,
                    shape = CircleShape
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 3.dp),
            verticalArrangement = Arrangement.Center,
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
                    .weight(1f)
            )


            Text(
                text = maybeBalanceToDisplay?.let { formatBalance(it) } ?: "",
                color = balanceColor,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

private val roundingToIntFormatter = RoundedToIntNumberFormatter()
private val numberFormatter = NumberFormatter.get()

private fun formatBalance(balance: Double): String {
    val roundedToInt = roundingToIntFormatter.format(-balance)
    return when {
        roundedToInt.length <= 4 -> roundedToInt
        else -> numberFormatter.format(-balance)
    }
}