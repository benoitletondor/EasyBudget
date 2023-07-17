/*
 *   Copyright 2023 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iab: Iab,
) : ViewModel() {
    val premiumStatusFlow: StateFlow<PremiumCheckStatus> = iab.iabStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PremiumCheckStatus.INITIALIZING)

    val isIabReady: Boolean get() = iab.isIabReady()
    val isUserPremium: Boolean get() = iab.isUserPremium()

    private val openPremiumEventMutableFlow = MutableLiveFlow<Unit>()
    val openPremiumEventFlow: Flow<Unit> = openPremiumEventMutableFlow

    fun onBecomePremiumButtonPressed() {
        viewModelScope.launch {
            openPremiumEventMutableFlow.emit(Unit)
        }
    }

    fun onWelcomeScreenFinished() {
        // TODO
    }
}