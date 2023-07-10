package com.benoitletondor.easybudgetapp.view.premium2.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenDarkColor
import com.benoitletondor.easybudgetapp.view.premium2.PremiumViewModel

@Composable
fun BoxScope.SubscribeView(viewModel: PremiumViewModel, premiumSubscribed: Boolean, proSubscribed: Boolean) {
    var selectedIndex by remember { mutableIntStateOf(if (!premiumSubscribed) 0 else 1) }

    Column {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = easyBudgetGreenDarkColor,
            contentColor = easyBudgetGreenDarkColor,
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 20.dp)
                .clip(RoundedCornerShape(50)),
            divider = { Box {} },
            indicator = { Box {} }
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

        if (selectedIndex == 0) {
            PremiumSubscriptionView(viewModel, premiumSubscribed)
        } else {
            ProSubscriptionView(viewModel, proSubscribed)
        }
    }
}

@Composable
private fun ColumnScope.PremiumSubscriptionView(viewModel: PremiumViewModel, premiumSubscribed: Boolean) {

}

@Composable
private fun ColumnScope.ProSubscriptionView(viewModel: PremiumViewModel, proSubscribed: Boolean) {

}