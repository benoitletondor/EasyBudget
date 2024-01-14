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

package com.benoitletondor.easybudgetapp.model

/**
 * Type of recurring expense.
 *
 * **Important**: do not change the order of those fields since its used to display choices to the user.
 *
 * @author Benoit LETONDOR
 */
enum class RecurringExpenseType {
    /**
     * An expense that occurs every day
     */
    DAILY,

    /**
     * An expense that occurs every week
     */
    WEEKLY,

    /**
     * An expense that occurs every 2 weeks
     */
    BI_WEEKLY,

    /**
     * An expense that occurs every 3 weeks
     */
    TER_WEEKLY,

    /**
     * An expense that occurs every 4 weeks
     */
    FOUR_WEEKLY,

    /**
     * An expense that occurs every month
     */
    MONTHLY,

    /**
     * An expense that occurs every 2 months
     */
    BI_MONTHLY,

    /**
     * An expense that occurs every 3 months
     */
    TER_MONTHLY,

    /**
     * An expense that occurs every 6 months
     */
    SIX_MONTHLY,

    /**
     * An expense that occurs once a year
     */
    YEARLY
}
