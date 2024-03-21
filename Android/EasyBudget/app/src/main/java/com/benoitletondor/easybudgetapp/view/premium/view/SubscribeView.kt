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

package com.benoitletondor.easybudgetapp.view.premium.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.iab.Pricing
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenDarkColor
import com.benoitletondor.easybudgetapp.view.premium.PremiumViewModel

private val starsYellowColor = Color(0xFFFEE101)
private val starsGreyColor = Color(0xFFD7D7D7)

@Composable
fun BoxScope.SubscribeView(
    pricing: Pricing,
    showProByDefault: Boolean,
    premiumSubscribed: Boolean,
    proSubscribed: Boolean,
    onCancelButtonClicked: () -> Unit,
    onBuyPremiumButtonClicked: () -> Unit,
    onBuyProButtonClicked: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(if (!premiumSubscribed && !showProByDefault) 0 else 1) }

    val starsColor by animateColorAsState(
        targetValue = if (selectedIndex == 0) starsGreyColor else starsYellowColor,
        label = "stars color animation",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(starsColor),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .rotate(45f)
                    .size(30.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                colorFilter = ColorFilter.tint(starsColor),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_star_yellow_48dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(starsColor),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .rotate(45f)
                    .size(30.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                .padding(vertical = 10.dp, horizontal = 20.dp)
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
                .padding(top = 16.dp, bottom = 20.dp)
                .fillMaxWidth()
                .weight(1.0f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val (x) = dragAmount
                        when {
                            x < 0 && x < 125 -> {
                                if (selectedIndex == 0) {
                                    selectedIndex++
                                }
                            }

                            x > 0 && x > 125 -> {
                                if (selectedIndex == 1) {
                                    selectedIndex--
                                }
                            }
                        }
                    }
                }
        ) {
            if (selectedIndex == 0) {
                PremiumSubscriptionView(premiumSubscribed)
            } else {
                ProSubscriptionView(proSubscribed)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onCancelButtonClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.weight(0.5f),
            ) {
                Text(
                    text = if ((selectedIndex == 0 && premiumSubscribed) || (selectedIndex == 1 && proSubscribed)) {
                        stringResource(R.string.premium_popup_become_go_back)
                    } else {
                        stringResource(R.string.premium_popup_become_not_now)
                    },
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }

            if ((selectedIndex == 0 && !premiumSubscribed) ||
                (selectedIndex == 1 && !proSubscribed))  {
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (selectedIndex == 0) {
                            onBuyPremiumButtonClicked()
                        } else {
                            onBuyProButtonClicked()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(0.5f),
                ) {
                    Text(
                        text = stringResource(if (premiumSubscribed) {
                            R.string.premium_popup_upgrade_cta
                        } else {
                            R.string.premium_popup_buy_cta
                        }),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 6.dp),
            text = stringResource(
                id = R.string.premium_screen_pricing_disclaimer,
                if (selectedIndex == 0) pricing.premiumPricing else pricing.proPricing
            ),
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.PremiumSubscriptionView(premiumSubscribed: Boolean) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.padding(bottom = 26.dp),
            text = stringResource(if (premiumSubscribed) R.string.premium_popup_premium_unlocked_description else R.string.premium_popup_premium_description),
            color = Color.White,
            fontSize = 16.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature2_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature2_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp),
            painter = painterResource(R.drawable.monthly_report),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_backup_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 20.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_backup_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature1_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature1_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp),
            painter = painterResource(R.drawable.daily_reminder),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_expense_check),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_expense_check_desc),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp),
            painter = painterResource(R.drawable.expense_check),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_dark_theme),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_premium_feature_more_dark_theme_desc),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            painter = painterResource(R.drawable.darkmode),
            contentDescription = null,
        )

    }
}

@Composable
private fun ColumnScope.ProSubscriptionView(proSubscribed: Boolean) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier.padding(bottom = 26.dp),
            text = stringResource(if (proSubscribed) R.string.premium_popup_pro_unlocked_description else R.string.premium_popup_pro_description),
            color = Color.White,
            fontSize = 16.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_pro_feature1_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 20.dp),
            text = stringResource(R.string.premium_popup_not_pro_feature1_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            painter = painterResource(R.drawable.savings),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_pro_feature2_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            modifier = Modifier.padding(bottom = 20.dp),
            text = stringResource(R.string.premium_popup_not_pro_feature2_message),
            color = Color.White,
            fontSize = 16.sp,
        )

        Image(
            painter = painterResource(R.drawable.users),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(R.string.premium_popup_not_pro_feature3_title),
            color = Color.White,
            fontSize = 20.sp,
        )

        Text(
            text = stringResource(R.string.premium_popup_not_pro_feature3_message),
            color = Color.White,
            fontSize = 16.sp,
        )

    }
}

private val stubPricing = Pricing(
    premiumPricing = "$2",
    proPricing = "$5",
)

@Composable
@Preview(showSystemUi = true)
private fun SubscribeToPremiumPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(easyBudgetGreenColor)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            SubscribeView(
                pricing = stubPricing,
                showProByDefault = false,
                premiumSubscribed = false,
                proSubscribed = false,
                onCancelButtonClicked = { },
                onBuyPremiumButtonClicked = {},
                onBuyProButtonClicked = {},
            )
        }
    }
}

@Composable
@Preview(showSystemUi = true)
private fun PremiumSubscribedPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(easyBudgetGreenColor)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            SubscribeView(
                pricing = stubPricing,
                showProByDefault = false,
                premiumSubscribed = true,
                proSubscribed = false,
                onCancelButtonClicked = { },
                onBuyPremiumButtonClicked = {},
                onBuyProButtonClicked = {},
            )
        }
    }
}

@Composable
@Preview(showSystemUi = true)
private fun ProSubscribedPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(easyBudgetGreenColor)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            SubscribeView(
                pricing = stubPricing,
                showProByDefault = false,
                premiumSubscribed = true,
                proSubscribed = true,
                onCancelButtonClicked = { },
                onBuyPremiumButtonClicked = {},
                onBuyProButtonClicked = {},
            )
        }
    }
}