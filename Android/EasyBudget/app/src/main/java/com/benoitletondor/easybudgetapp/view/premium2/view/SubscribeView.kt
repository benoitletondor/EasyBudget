package com.benoitletondor.easybudgetapp.view.premium2.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenDarkColor
import com.benoitletondor.easybudgetapp.view.premium2.PremiumViewModel

private val starsYellowColor = Color(0xFFFEE101)
private val starsGreyColor = Color(0xFFD7D7D7)

@Composable
fun BoxScope.SubscribeView(
    viewModel: PremiumViewModel,
    premiumSubscribed: Boolean,
    proSubscribed: Boolean,
    onCancelButtonClicked: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(if (!premiumSubscribed) 0 else 1) }

    val starsColor by animateColorAsState(
        if (selectedIndex == 0) starsGreyColor else starsYellowColor
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(starsColor),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .rotate(45f),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                colorFilter = ColorFilter.tint(starsColor),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(starsColor),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .rotate(45f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "EasyBudget " + if(selectedIndex == 0) { stringResource(R.string.settings_subscribe_premium) } else { stringResource(R.string.settings_subscribe_pro) },
            color = Color.White,
            fontSize = 23.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = easyBudgetGreenDarkColor,
            contentColor = easyBudgetGreenDarkColor,
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 20.dp)
                .clip(RoundedCornerShape(50)),
            divider = { },
            indicator = { },
        ) {
            (0..1).forEach { index ->
                val selected = selectedIndex == index
                Tab(
                    modifier = if (selected) Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                    else Modifier
                        .clip(RoundedCornerShape(50))
                        .background(easyBudgetGreenDarkColor),
                    selected = selected,
                    onClick = { selectedIndex = index },
                    text = {
                        val color = if (selectedIndex == index) easyBudgetGreenColor else Color.White
                        when(index) {
                            0 -> Text(text = stringResource(R.string.settings_subscribe_premium), color = color)
                            1 -> Text(text = stringResource(R.string.settings_subscribe_pro), color = color)
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .weight(1.0f),
        ) {
            if (selectedIndex == 0) {
                PremiumSubscriptionView(viewModel, premiumSubscribed)
            } else {
                ProSubscriptionView(viewModel, proSubscribed)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Button(
                onClick = onCancelButtonClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.weight(0.5f),
            ) {
                Text(
                    text = "Not now",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ),
                modifier = Modifier.weight(0.5f),
            ) {
                Text(
                    text = "Purchase",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PremiumSubscriptionView(viewModel: PremiumViewModel, premiumSubscribed: Boolean) {

}

@Composable
private fun ColumnScope.ProSubscriptionView(viewModel: PremiumViewModel, proSubscribed: Boolean) {

}