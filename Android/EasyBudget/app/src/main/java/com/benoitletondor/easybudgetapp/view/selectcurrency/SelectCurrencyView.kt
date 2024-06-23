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
package com.benoitletondor.easybudgetapp.view.selectcurrency

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Currency

@Composable
fun SelectCurrencyDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.window_background),
            ),
        ) {
            SelectCurrencyView(
                modifier = Modifier.fillMaxSize(),
                onCurrencySelected = onDismissRequest,
            )
        }
    }
}

@Composable
fun SelectCurrencyView(
    modifier: Modifier = Modifier,
    viewModel: SelectCurrencyViewModel = hiltViewModel(),
    onCurrencySelected: () -> Unit = {},
) {
    SelectCurrencyView(
        modifier = modifier,
        stateFlow = viewModel.stateFlow,
        onCurrencySelected = { currency ->
            viewModel.onCurrencySelected(currency)
            onCurrencySelected()
        },
    )
}

@Composable
private fun SelectCurrencyView(
    modifier: Modifier,
    stateFlow: StateFlow<SelectCurrencyViewModel.State>,
    onCurrencySelected: (Currency) -> Unit,
) {
    val state by stateFlow.collectAsState()

    when (val currentState = state) {
        is SelectCurrencyViewModel.State.Loading -> LoadingView(
            modifier = modifier,
        )
        is SelectCurrencyViewModel.State.Loaded -> CurrenciesView(
            modifier = modifier,
            state = currentState,
            onCurrencySelected = onCurrencySelected,
        )
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun CurrenciesView(
    modifier: Modifier,
    state: SelectCurrencyViewModel.State.Loaded,
    onCurrencySelected: (Currency) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(
            count = state.mainCurrencies.size + state.otherCurrencies.size,
        ) { index ->
            val currencyItem = if (index < state.mainCurrencies.size) {
                state.mainCurrencies[index]
            } else {
                state.otherCurrencies[index - state.mainCurrencies.size]
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ){
                if (index == state.mainCurrencies.size) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = colorResource(R.color.divider),
                        thickness = 3.dp,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCurrencySelected(currencyItem.currency)
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_check_green_24dp),
                        contentDescription = stringResource(androidx.compose.ui.R.string.selected),
                        modifier = Modifier.alpha(if (currencyItem.isSelected) 1f else 0f)
                    )

                    Spacer(modifier = Modifier.padding(6.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = CurrencyHelper.getCurrencyDisplayName(currencyItem.currency),
                    )
                }

                if (index < state.mainCurrencies.size - 1 + state.otherCurrencies.size) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = colorResource(R.color.divider),
                        thickness = 1.dp,
                    )
                }
            }
        }
    }
}