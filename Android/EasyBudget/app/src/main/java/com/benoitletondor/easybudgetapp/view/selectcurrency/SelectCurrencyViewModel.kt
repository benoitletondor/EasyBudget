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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SelectCurrencyViewModel @Inject constructor() : ViewModel() {
    private val stateMutableFlow = MutableStateFlow<State>(State.Loading)
    val stateFlow: Flow<State> = stateMutableFlow

    init {
        viewModelScope.launch {
            val (mainCurrencies, otherCurrencies) = withContext(Dispatchers.Default) {
                Pair(CurrencyHelper.getMainAvailableCurrencies(), CurrencyHelper.getOtherAvailableCurrencies())
            }

            stateMutableFlow.emit(State.Loaded(mainCurrencies, otherCurrencies))
        }
    }

    sealed class State {
        data object Loading : State()
        data class Loaded(val mainCurrencies: List<Currency>, val otherCurrencies: List<Currency>) : State()
    }
}