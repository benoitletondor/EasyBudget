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

package com.benoitletondor.easybudgetapp.view.report

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Type of cell for an [Expense]
 */
private const val EXPENSE_VIEW_TYPE = 1
/**
 * Type of cell for a header
 */
private const val HEADER_VIEW_TYPE = 2

/**
 * The adapter for the [MonthlyReportFragment] recycler view.
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportRecyclerViewAdapter(private val expenses: List<Expense>,
                                       private val revenues: List<Expense>,
                                       private val parameters: Parameters) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Formatter to get day number for each date
     */
    private val dayFormatter = DateTimeFormatter.ofPattern("dd", Locale.getDefault())

// --------------------------------------->

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (HEADER_VIEW_TYPE == viewType) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_monthly_report_header_cell, parent, false)
            return HeaderViewHolder(v)
        }

        val v = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_monthly_report_expense_cell, parent, false)
        return ExpenseViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val isRevenuesHeader = isRevenuesHeader(position)

            holder.headerTitle.setText(if (isRevenuesHeader) R.string.revenues else R.string.expenses)
            holder.view.setBackgroundColor(ContextCompat.getColor(holder.view.context, if (isRevenuesHeader) R.color.budget_green else R.color.budget_red))
        } else {
            val viewHolder = holder as ExpenseViewHolder
            val expense = getExpense(position)

            viewHolder.expenseTitleTextView.text = expense.title
            viewHolder.expenseAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, -expense.amount)
            viewHolder.expenseAmountTextView.setTextColor(ContextCompat.getColor(viewHolder.view.context, if (expense.isRevenue()) R.color.budget_green else R.color.budget_red))
            viewHolder.monthlyIndicator.visibility = if (expense.isRecurring()) View.VISIBLE else View.GONE

            if (expense.isRecurring()) {
                when (expense.associatedRecurringExpense!!.recurringExpense.type) {
                    RecurringExpenseType.DAILY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.daily)
                    RecurringExpenseType.WEEKLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.weekly)
                    RecurringExpenseType.BI_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.bi_weekly)
                    RecurringExpenseType.TER_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.ter_weekly)
                    RecurringExpenseType.FOUR_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.four_weekly)
                    RecurringExpenseType.MONTHLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.monthly)
                    RecurringExpenseType.BI_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.bi_monthly)
                    RecurringExpenseType.TER_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.ter_monthly)
                    RecurringExpenseType.SIX_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.six_monthly)
                    RecurringExpenseType.YEARLY -> viewHolder.recurringExpenseTypeTextView.text = viewHolder.view.context.getString(R.string.yearly)
                }
            }

            viewHolder.dateTextView.text = dayFormatter.format(expense.date)
        }
    }

    override fun getItemCount() = (if (expenses.isEmpty()) 0 else expenses.size + 1) + if (revenues.isEmpty()) 0 else revenues.size + 1

    override fun getItemViewType(position: Int) = if (isHeader(position)) HEADER_VIEW_TYPE else EXPENSE_VIEW_TYPE

    /**
     * Get the expense for the given position
     *
     * @param position the position
     * @return the expense for that position
     */
    private fun getExpense(position: Int): Expense {
        if (revenues.isNotEmpty() && position - 1 < revenues.size) {
            return revenues[position - 1]
        }

        val expensesHeaderDelta = 1 + if (revenues.isEmpty()) 0 else 1
        return expenses[position - expensesHeaderDelta - revenues.size]
    }

    /**
     * Is the given position an header cell
     *
     * @param position the position
     * @return true if it's an header, false otherwise
     */
    private fun isHeader(position: Int): Boolean {
        return isExpensesHeader(position) || isRevenuesHeader(position)
    }

    /**
     * Is the given position the expense header cell
     *
     * @param position the position
     * @return true if it's the expense header, false otherwise
     */
    private fun isExpensesHeader(position: Int): Boolean {
        return expenses.isNotEmpty() && position == revenues.size + if (revenues.isEmpty()) 0 else 1
    }

    /**
     * Is the given position the revenue header cell
     *
     * @param position the position
     * @return true if it's the revenue header, false otherwise
     */
    private fun isRevenuesHeader(position: Int): Boolean {
        return revenues.isNotEmpty() && position == 0
    }

// --------------------------------------->

    class ExpenseViewHolder internal constructor(internal val view: View) : RecyclerView.ViewHolder(view) {
        internal val expenseTitleTextView: TextView = view.findViewById(R.id.expense_title)
        internal val expenseAmountTextView: TextView = view.findViewById(R.id.expense_amount)
        internal val monthlyIndicator: ViewGroup = view.findViewById(R.id.recurring_indicator)
        internal val dateTextView: TextView = view.findViewById(R.id.date_tv)
        internal val recurringExpenseTypeTextView: TextView = view.findViewById(R.id.recurring_expense_type)
    }

    class HeaderViewHolder internal constructor(internal val view: View) : RecyclerView.ViewHolder(view) {
        internal val headerTitle: TextView = view.findViewById(R.id.monthly_recycler_view_header_tv)
    }
}
