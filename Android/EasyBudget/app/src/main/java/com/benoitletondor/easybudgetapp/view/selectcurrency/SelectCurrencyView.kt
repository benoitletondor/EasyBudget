package com.benoitletondor.easybudgetapp.view.selectcurrency

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Currency

@Composable
fun SelectCurrencyView(
    modifier: Modifier = Modifier,
    viewModel: SelectCurrencyViewModel = hiltViewModel(),
) {
    SelectCurrencyView(
        modifier = modifier,
        stateFlow = viewModel.stateFlow,
        onCurrencySelected = viewModel::onCurrencySelected,
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
    LaunchedEffect(key1 = "firstEmit") {
        onCurrencySelected(state.mainCurrencies.firstOrNull { it.isSelected }?.currency ?: state.otherCurrencies.first { it.isSelected }.currency)
    }

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