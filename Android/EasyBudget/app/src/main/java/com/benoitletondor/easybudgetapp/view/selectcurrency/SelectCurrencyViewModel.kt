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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.getUserCurrency
import com.benoitletondor.easybudgetapp.helper.setUserCurrency
import com.benoitletondor.easybudgetapp.parameters.Parameters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SelectCurrencyViewModel @Inject constructor(
    private val parameters: Parameters,
) : ViewModel() {
    private val stateMutableFlow = MutableStateFlow<State>(State.Loading)
    val stateFlow: StateFlow<State> = stateMutableFlow

    init {
        viewModelScope.launch {
            val (mainCurrencies, otherCurrencies) = withContext(Dispatchers.Default) {
                Pair(CurrencyHelper.getMainAvailableCurrencies(), CurrencyHelper.getOtherAvailableCurrencies())
            }

            val userCurrency = parameters.getUserCurrency()

            stateMutableFlow.emit(State.Loaded(
                mainCurrencies = mainCurrencies.map {
                    CurrencyItem(
                        currency = it,
                        isSelected = it == userCurrency,
                    )
                },
                otherCurrencies = otherCurrencies.map {
                    CurrencyItem(
                        currency = it,
                        isSelected = it == userCurrency,
                    )
                },
            ))
        }
    }

    fun onCurrencySelected(currency: Currency) {
        viewModelScope.launch {
            parameters.setUserCurrency(currency)

            val currentState = stateFlow.value as? State.Loaded ?: return@launch
            stateMutableFlow.emit(currentState.copy(
                mainCurrencies = currentState.mainCurrencies.map {
                    it.copy(isSelected = it.currency == currency)
                },
                otherCurrencies = currentState.otherCurrencies.map {
                    it.copy(isSelected = it.currency == currency)
                },
            ))
        }
    }

    sealed class State {
        data object Loading : State()
        @Immutable
        data class Loaded(
            val mainCurrencies: List<CurrencyItem>,
            val otherCurrencies: List<CurrencyItem>,
        ) : State()
    }

    @Immutable
    data class CurrencyItem(
        val currency: Currency,
        val isSelected: Boolean,
    )
}