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

import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.view.main.account.calendar.NumberFormatter
import com.benoitletondor.easybudgetapp.view.main.account.calendar.RoundedToIntNumberFormatter

@Composable
fun BoxScope.InCalendarWithBalanceDayView(
    size: Dp,
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
        size = size,
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
fun BoxScope.InCalendarEmptyDayView(
    size: Dp,
    dayOfMonth: Int,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    InCalendarDayView(
        size = size,
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
fun BoxScope.OffCalendarWithBalanceDayView(
    size: Dp,
    dayOfMonth: Int,
    balanceToDisplay: Double,
    lowMoneyWarningAmount: Int,
    displayUncheckedStyle: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    OffCalendarDayView(
        size = size,
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
fun BoxScope.OffCalendarEmptyDayView(
    size: Dp,
    dayOfMonth: Int,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    OffCalendarDayView(
        size = size,
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
private fun BoxScope.InCalendarDayView(
    size: Dp,
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
        size = size,
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
private fun BoxScope.OffCalendarDayView(
    size: Dp,
    dayOfMonth: Int,
    maybeBalanceToDisplay: Double?,
    lowMoneyWarningAmount: Int?,
    displayUncheckedStyle: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    CalendarDayView(
        size = size,
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
private fun BoxScope.CalendarDayView(
    size: Dp,
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
    Column(
        modifier = Modifier
            .width(size)
            .aspectRatio(1f)
            .align(Alignment.Center)
            .clip(shape = CircleShape)
            .background(
                color = if (selected) {
                    colorResource(id = R.color.calendar_cell_selected)
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = 2.dp,
                color = if (today) colorResource(id = R.color.calendar_today_stroke_color) else Color.Transparent,
                shape = CircleShape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dayOfMonth.toString(),
            color = dayOfMonthColor,
            fontStyle = dayOfMonthFontStyle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Visible,
            maxLines = 1,
            style = TextStyle(
                // https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.0-alpha01
                platformStyle = PlatformTextStyle(
                    includeFontPadding = true,
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .wrapContentHeight(align = Alignment.Bottom)
        )

        Text(
            text = maybeBalanceToDisplay?.let { formatBalance(it) } ?: "",
            color = balanceColor,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(
                // https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.0-alpha01
                platformStyle = PlatformTextStyle(
                    includeFontPadding = true,
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
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

@Composable
@Preview(name = "Normal light", widthDp = 1000)
@Preview(name = "Normal dark", widthDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DayViewPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreviewRowForWidth(width = 300.dp)
            PreviewRowForWidth(width = 350.dp)
            PreviewRowForWidth(width = 400.dp)
            PreviewRowForWidth(width = 500.dp)
        }
    }
}

@Composable
private fun PreviewRowForWidth(width: Dp) {
    Row(
        modifier = Modifier
            .width(width)
            .wrapContentHeight()
            .background(colorResource(id = R.color.window_background)),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayViewsForPreview()
    }
}

@Composable
private fun RowScope.DayViewsForPreview() {
    val density = LocalDensity.current
    val size = remember {
        with(density) {
            40.sp.toDp()
        }
    }


    DayViewForPreview {
        OffCalendarEmptyDayView(
            size = size,
            dayOfMonth = 25,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        OffCalendarEmptyDayView(
            size = size,
            dayOfMonth = 26,
            today = true,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        OffCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 27,
            balanceToDisplay = -500.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = false,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        OffCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 28,
            balanceToDisplay = 50.0,
            lowMoneyWarningAmount = 1000,
            displayUncheckedStyle = false,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        OffCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 29,
            balanceToDisplay = 5.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = true,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        OffCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 30,
            balanceToDisplay = 5000.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = false,
            today = true,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarEmptyDayView(
            size = size,
            dayOfMonth = 1,
            selected = false,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 2,
            balanceToDisplay = -500.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = false,
            selected = false,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 3,
            balanceToDisplay = -500.0,
            lowMoneyWarningAmount = 600,
            displayUncheckedStyle = false,
            selected = false,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 4,
            balanceToDisplay = 500.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = true,
            selected = true,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 5,
            balanceToDisplay = 5000.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = false,
            selected = true,
            today = false,
            onClick = {},
            onLongClick = {},
        )
    }

    DayViewForPreview {
        InCalendarWithBalanceDayView(
            size = size,
            dayOfMonth = 6,
            balanceToDisplay = 500.0,
            lowMoneyWarningAmount = 100,
            displayUncheckedStyle = false,
            selected = false,
            today = true,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Composable
private fun RowScope.DayViewForPreview(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clipToBounds(),
    ) {
        content()
    }
}