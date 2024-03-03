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
package com.benoitletondor.easybudgetapp.view.main.account

import androidx.compose.runtime.Composable
import com.benoitletondor.easybudgetapp.view.main.account.calendar.CalendarView

@Composable
fun AccountView(
    viewModel: AccountViewModel,
) {
    // TODO loading state
    CalendarView(
        parameters = viewModel.parameters,
        dbAvailableFlow = viewModel.dbAvailableFlow,
        forceRefreshDataFlow = viewModel.forceRefreshFlow,
        selectedDateFlow = viewModel.selectDateFlow,
        includeCheckedBalanceFlow = viewModel.includeCheckedBalanceFlow,
        onMonthChanged = viewModel::onMonthChanged,
        goBackToCurrentMonthEventFlow = viewModel.goBackToCurrentMonthEventFlow,
        onDateSelected = viewModel::onSelectDate,
        onDateLongClicked = viewModel::onDateLongClicked,
    )
}