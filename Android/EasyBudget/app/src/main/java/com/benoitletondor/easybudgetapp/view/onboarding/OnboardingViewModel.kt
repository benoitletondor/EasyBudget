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
package com.benoitletondor.easybudgetapp.view.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.helper.MutableLiveFlow
import com.benoitletondor.easybudgetapp.helper.watchUserCurrency
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.parameters.ONBOARDING_STEP_COMPLETED
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.setOnboardingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val parameters: Parameters,
    private val db: DB,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableEventFlow: MutableLiveFlow<Event> = MutableLiveFlow()
    val eventFlow: Flow<Event> = mutableEventFlow

    private val userMoneyAmountMutableFlow = MutableStateFlow(0.0)
    val userMoneyAmountFlow: StateFlow<Double> = userMoneyAmountMutableFlow

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userMoneyAmountMutableFlow.value = -db.getBalanceForDay(LocalDate.now())
        }
    }

    val userCurrencyFlow get() = parameters.watchUserCurrency()

    fun onBackPressed(page: OnboardingPage) {
        viewModelScope.launch {
            if (page == OnboardingPage.WELCOME) {
                mutableEventFlow.emit(Event.FinishWithResult(OnboardingResult(onboardingCompleted = false)))
            } else {
                mutableEventFlow.emit(Event.GoToPreviousPage)
            }
        }
    }

    fun onNextButtonPressed(page: OnboardingPage) {
        viewModelScope.launch {
            when(page) {
                OnboardingPage.WELCOME,
                OnboardingPage.CURRENCY,
                OnboardingPage.PUSH_NOTIFICATIONS -> mutableEventFlow.emit(Event.GoToNextPage)
                OnboardingPage.INITIAL_AMOUNT -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentBalance = -db.getBalanceForDay(LocalDate.now())
                        val amountParsed = userMoneyAmountMutableFlow.value
                        if (amountParsed != currentBalance) {
                            val diff = amountParsed - currentBalance

                            val existingBalanceExpense = db.getExpensesForDay(LocalDate.now()).firstOrNull { it.title == context.getString(R.string.adjust_balance_expense_title) }
                            val expense = existingBalanceExpense?.copy(amount = -diff)
                                ?: Expense(context.getString(R.string.adjust_balance_expense_title), -diff, LocalDate.now(), true)

                            db.persistExpense(expense)
                        }

                        withContext(Dispatchers.Main) {
                            mutableEventFlow.emit(Event.GoToNextPage)
                        }
                    }
                }
                OnboardingPage.END -> {
                    parameters.setOnboardingStep(ONBOARDING_STEP_COMPLETED)
                    mutableEventFlow.emit(Event.FinishWithResult(OnboardingResult(onboardingCompleted = true)))
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        val amountParsed = parseAmountValue(amount)
        userMoneyAmountMutableFlow.value = amountParsed
    }

    fun onAcceptNotificationsPressed() {
        viewModelScope.launch {
            mutableEventFlow.emit(Event.RequestPushPermission)
        }
    }

    fun onDenyNotificationsPressed() {
        viewModelScope.launch {
            mutableEventFlow.emit(Event.GoToNextPage)
        }
    }

    fun onPushNotificationsResponse(onboardingPage: OnboardingPage) {
        if (onboardingPage == OnboardingPage.PUSH_NOTIFICATIONS) {
            viewModelScope.launch {
                mutableEventFlow.emit(Event.GoToNextPage)
            }
        }
    }

    sealed class Event {
        data class FinishWithResult(val result: OnboardingResult) : Event()
        data object GoToPreviousPage : Event()
        data object GoToNextPage : Event()
        data object RequestPushPermission : Event()
    }

    enum class OnboardingPage {
        WELCOME,
        CURRENCY,
        INITIAL_AMOUNT,
        PUSH_NOTIFICATIONS,
        END,
    }
}

private fun parseAmountValue(valueString: String): Double {
    if ( "" == valueString || "-" == valueString || "." == valueString || "," == valueString ) {
        return 0.0
    }

    return try {
        java.lang.Double.valueOf(valueString.replace(",", "."))
    } catch (e: Exception) {
        Logger.debug("An error occurred during initial amount parsing first pass: $valueString", e)

        try {
            return valueString.toDouble()
        } catch (subException: Exception) {
            Logger.warning("An error occurred during initial amount parsing second pass: $valueString", e)
            return 0.0
        }
    }
}
