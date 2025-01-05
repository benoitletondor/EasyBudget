/*
 *   Copyright 2025 Benoit Letondor
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
package com.benoitletondor.easybudgetapp.helper

import android.content.Context
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType

fun RecurringExpenseType.stringRepresentation(context: Context): String {
    return when (this) {
        RecurringExpenseType.DAILY -> context.getString(R.string.recurring_interval_daily)
        RecurringExpenseType.WEEKLY -> context.getString(R.string.recurring_interval_weekly)
        RecurringExpenseType.BI_WEEKLY -> context.getString(R.string.recurring_interval_bi_weekly)
        RecurringExpenseType.TER_WEEKLY -> context.getString(R.string.recurring_interval_ter_weekly)
        RecurringExpenseType.FOUR_WEEKLY -> context.getString(R.string.recurring_interval_four_weekly)
        RecurringExpenseType.MONTHLY -> context.getString(R.string.recurring_interval_monthly)
        RecurringExpenseType.BI_MONTHLY -> context.getString(R.string.recurring_interval_bi_monthly)
        RecurringExpenseType.TER_MONTHLY -> context.getString(R.string.recurring_interval_ter_monthly)
        RecurringExpenseType.SIX_MONTHLY -> context.getString(R.string.recurring_interval_six_monthly)
        RecurringExpenseType.YEARLY -> context.getString(R.string.recurring_interval_yearly)
    }
}