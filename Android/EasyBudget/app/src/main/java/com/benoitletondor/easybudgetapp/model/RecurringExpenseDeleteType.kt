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
 * An enum that reference different kind of deletion for a recurring expense
 *
 * @author Benoit LETONDOR
 */
enum class RecurringExpenseDeleteType(val value: Int) {
    /**
     * Delete all from a date
     */
    FROM(0),

    /**
     * Delete all before a date
     */
    TO(1),

    /**
     * Delete all
     */
    ALL(2),

    /**
     * Delete this expense occurrence only
     */
    ONE(3);


    companion object {
        /**
         * Retrieve the enum for the given value
         *
         * @param value
         * @return
         */
        fun fromValue(value: Int): RecurringExpenseDeleteType? {
            for (type in entries) {
                if (value == type.value) {
                    return type
                }
            }

            return null
        }
    }
}