package com.benoitletondor.easybudgetapp.view.main.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R

@Composable
fun FABMenuOverlay(
    onAddRecurringEntryPressed: () -> Unit,
    onAddEntryPressed: () -> Unit,
    onTapOutsideCTAs: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.menu_background_overlay_color))
            .padding(bottom = 90.dp, end = 16.dp)
            .clickable(
                onClick = onTapOutsideCTAs,
                indication = null,
                interactionSource = null,
            ),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onAddRecurringEntryPressed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color = Color.Black)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                text = stringResource(R.string.fab_add_monthly_expense),
                color = Color.White,
                fontSize = 15.sp,
            )

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = onAddRecurringEntryPressed,
                containerColor = colorResource(R.color.fab_add_monthly_expense),
                contentColor = colorResource(R.color.white),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_autorenew_white),
                    contentDescription = stringResource(R.string.fab_add_monthly_expense),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.clickable(onClick = onAddEntryPressed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color = Color.Black)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                text = stringResource(R.string.fab_add_expense),
                color = Color.White,
                fontSize = 15.sp,
            )

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = onAddEntryPressed,
                containerColor = colorResource(R.color.fab_add_expense),
                contentColor = colorResource(R.color.white),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = stringResource(R.string.fab_add_expense),
                )
            }
        }
    }
}