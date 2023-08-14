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

package com.benoitletondor.easybudgetapp.view.main.account.calendar

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getLowMoneyWarningAmount
import com.benoitletondor.easybudgetapp.parameters.getShouldShowCheckedBalance
import com.roomorama.caldroid.CaldroidGridAdapter
import com.roomorama.caldroid.CalendarHelper

import java.time.LocalDate

/**
 * @author Benoit LETONDOR
 */
class CalendarGridAdapter(
    context: Context,
    private val dataProvider: CalendarGridAdapterDataProvider,
    private val parameters: Parameters,
    month: Int,
    year: Int,
    caldroidData: Map<String, Any>,
    extraData: Map<String, Any>,
) : CaldroidGridAdapter(context, month, year, caldroidData, extraData) {

    private val roundingToIntFormatter = RoundedToIntNumberFormatter()
    private val formatter = NumberFormatter.get()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val cellView = convertView ?: createView(parent)

        val viewData = cellView.tag as ViewData

        // Get dateTime of this cell
        val dateTime = this.datetimeList[position]!!
        val isToday = dateTime == getToday()
        val isDisabled = minDateTime != null && dateTime.lt(minDateTime) || maxDateTime != null && dateTime.gt(maxDateTime) || disableDates != null && disableDatesMap.containsKey(dateTime)
        val isOutOfMonth = dateTime.month != month

        val tv1 = viewData.dayTextView
        val tv2 = viewData.amountTextView

        // Set today's date
        tv1.text = dateTime.day.toString()

        // Customize for disabled dates and date outside min/max dates
        if (isDisabled) {
            if (!viewData.isDisabled) {
                tv1.setTextColor(ContextCompat.getColor(context, R.color.calendar_cell_disabled_text_color))
                tv2.visibility = View.INVISIBLE
                cellView.setBackgroundResource(R.color.calendar_cell_background)

                viewData.isDisabled = true
            }
        } else if (viewData.isDisabled) { // Reset all view params
            tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
            tv2.text = ""
            tv2.visibility = View.VISIBLE
            cellView.setBackgroundResource(R.drawable.custom_grid_cell_drawable)

            viewData.isDisabled = false
            viewData.isSelected = false
            viewData.isToday = false
            viewData.containsExpenses = false
            viewData.colorIndicatorMarginForToday = false
            viewData.isOutOfMonth = false
        }

        if (!isDisabled) {
            if (isOutOfMonth) {
                if (!viewData.isOutOfMonth) {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.divider))
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.divider))

                    viewData.isOutOfMonth = true
                }
            } else if (viewData.isOutOfMonth) {
                tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
                tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))

                viewData.isOutOfMonth = false
            }

            // Today's cell
            if (isToday) {
                // Customize for selected dates
                if (selectedDates != null && selectedDatesMap.containsKey(dateTime)) {
                    if (!viewData.isToday || !viewData.isSelected) {
                        cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_selected_drawable)

                        viewData.isToday = true
                        viewData.isSelected = true
                    }
                } else if (!viewData.isToday || viewData.isSelected) {
                    cellView.setBackgroundResource(R.drawable.custom_grid_today_cell_drawable)

                    viewData.isToday = true
                    viewData.isSelected = false
                }
            } else {
                // Customize for selected dates
                if (selectedDates != null && selectedDatesMap.containsKey(dateTime)) {
                    if (viewData.isToday || !viewData.isSelected) {
                        cellView.setBackgroundResource(R.drawable.custom_grid_cell_selected_drawable)

                        viewData.isToday = false
                        viewData.isSelected = true
                    }
                } else if (viewData.isToday || viewData.isSelected) {
                    cellView.setBackgroundResource(R.drawable.custom_grid_cell_drawable)

                    viewData.isToday = false
                    viewData.isSelected = false
                }
            }

            val date = CalendarHelper.convertDateTimeToDate(dateTime)
            if (dataProvider.hasExpenseForDay(date)) {
                val hasUnchecked = if (parameters.getShouldShowCheckedBalance()) {
                    dataProvider.hasUncheckedExpenseForDay(date)
                } else {
                    false
                }

                val balance = dataProvider.getBalanceForDay(date)

                if (!viewData.containsExpenses) {
                    tv2.visibility = View.VISIBLE

                    viewData.containsExpenses = true
                }

                tv2.text = formatBalance(balance)

                tv1.setTypeface(null, if (hasUnchecked) Typeface.ITALIC else Typeface.BOLD)
                tv1.setTextColor(ContextCompat.getColor(tv1.context, when {
                    -balance <= 0 -> R.color.budget_red
                    -balance < parameters.getLowMoneyWarningAmount() -> R.color.budget_orange
                    else -> R.color.budget_green
                }))
            } else if (viewData.containsExpenses) {
                tv2.visibility = View.INVISIBLE
                tv1.setTypeface(null, Typeface.BOLD)

                if (!isOutOfMonth) {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                } else {
                    tv1.setTextColor(ContextCompat.getColor(context, R.color.divider))
                    tv2.setTextColor(ContextCompat.getColor(context, R.color.divider))
                }

                viewData.containsExpenses = false
            }
        }

        cellView.tag = viewData
        return cellView
    }

    private fun formatBalance(balance: Double): String {
        val roundedToInt = roundingToIntFormatter.format(-balance)
        return when {
            roundedToInt.length <= 4 -> roundedToInt
            else -> formatter.format(-balance)
        }
    }

    /**
     * Inflate a new cell view and attach ViewData as tag
     */
    private fun createView(parent: ViewGroup): View {
        val v = LayoutInflater.from(context).inflate(R.layout.custom_grid_cell, parent, false)
        val viewData = ViewData(v.findViewById(R.id.grid_cell_tv1), v.findViewById(R.id.grid_cell_tv2))

        v.tag = viewData

        return v
    }

// --------------------------------------->

    /**
     * Object that represent data of a cell for optimization purpose
     */
    class ViewData(val dayTextView: TextView, val amountTextView: TextView) {
        /**
         * Is this cell a disabled date
         */
        var isDisabled = false
        /**
         * Is this cell out of the current month
         */
        var isOutOfMonth = false
        /**
         * Is this cell today's cell
         */
        var isToday = false
        /**
         * Is this cell selected
         */
        var isSelected = false
        /**
         * Does this cell contain expenses
         */
        var containsExpenses = false
        /**
         * Are color indicator margin set for today
         */
        var colorIndicatorMarginForToday = false
    }
}

interface CalendarGridAdapterDataProvider {
    fun hasExpenseForDay(dayDate: LocalDate): Boolean
    fun hasUncheckedExpenseForDay(dayDate: LocalDate): Boolean
    fun getBalanceForDay(dayDate: LocalDate): Double
}