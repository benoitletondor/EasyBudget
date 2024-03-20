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

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.toFormattedString
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

/**
 * Recycler view adapter to display expenses for a given date
 *
 * @author Benoit LETONDOR
 */
class ExpensesRecyclerViewAdapter(
    private val fragment: Fragment,
    private val parameters: Parameters,
    private var date: LocalDate,
    private val onExpenseCheckedListener: (Expense, Boolean) -> Unit,
) : RecyclerView.Adapter<ExpensesRecyclerViewAdapter.ViewHolder>() {

    private var expenses = mutableListOf<Expense>()
    private var isUserPremium = false

    fun setUserPremium(isPremium: Boolean) {
        if (isPremium == isUserPremium) {
            return
        }

        isUserPremium = isPremium
        notifyDataSetChanged()
    }

    /**
     * Set a new date and data to display
     */
    fun setDate(date: LocalDate, expenses: List<Expense>) {
        this.date = date
        this.expenses.clear()
        this.expenses.addAll(expenses)
        notifyDataSetChanged()
    }

// ------------------------------------------>

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.recycleview_expense_cell, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val expense = expenses[i]

        viewHolder.expenseTitleTextView.text = expense.title
        viewHolder.expenseAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, -expense.amount)
        viewHolder.expenseAmountTextView.setTextColor(ContextCompat.getColor(viewHolder.view.context, if (expense.isRevenue()) R.color.budget_green else R.color.budget_red))
        viewHolder.recurringIndicator.visibility = if (expense.isRecurring()) View.VISIBLE else View.GONE
        viewHolder.positiveIndicator.setImageResource(if (expense.isRevenue()) R.drawable.ic_label_green else R.drawable.ic_label_red)
        viewHolder.checkedCheckBox.visibility = if( isUserPremium ) { View.VISIBLE } else { View.GONE }
        viewHolder.checkedCheckBox.setOnCheckedChangeListener { _, checked ->
            if( checked != expense.checked ) {
                onExpenseCheckedListener(expense, checked)
            }
        }
        viewHolder.checkedCheckBox.isChecked = expense.checked

        if (expense.isRecurring()) {
            viewHolder.recurringIndicatorTextview.text = expense.associatedRecurringExpense!!.recurringExpense.type.toFormattedString(viewHolder.view.context)
        }

        val onClickListener = View.OnClickListener {
            if (expense.isRecurring()) {
                val builder = MaterialAlertDialogBuilder(fragment.requireContext())
                builder.setTitle(if (expense.isRevenue()) R.string.dialog_edit_recurring_income_title else R.string.dialog_edit_recurring_expense_title)
                builder.setItems(if (expense.isRevenue()) R.array.dialog_edit_recurring_income_choices else R.array.dialog_edit_recurring_expense_choices) { _, which ->
                    when (which) {
                        // Edit this one
                        0 -> {
                            val startIntent = ExpenseEditActivity.newIntent(
                                context = viewHolder.view.context,
                                editedExpense = expense,
                                date = expense.date,
                            )

                            fragment.requireActivity().startActivity(startIntent)
                        }
                        // Edit this one and following ones
                        1 -> {
                            val startIntent = RecurringExpenseEditActivity.newIntent(
                                context = viewHolder.view.context,
                                editedExpense = expense,
                                startDate = expense.date,
                            )

                            fragment.requireActivity().startActivity(startIntent)
                        }
                        // Delete this one
                        2 -> {
                            // Send notification to inform views that this expense has been deleted
                            val intent = Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED)
                            intent.putExtra("expense", expense)
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.ONE.value)
                            LocalBroadcastManager.getInstance(fragment.requireContext()).sendBroadcast(intent)
                        }
                        // Delete from
                        3 -> {
                            // Send notification to inform views that this expense has been deleted
                            val intent = Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED)
                            intent.putExtra("expense", expense)
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.FROM.value)
                            LocalBroadcastManager.getInstance(fragment.requireContext()).sendBroadcast(intent)
                        }
                        // Delete up to
                        4 -> {
                            // Send notification to inform views that this expense has been deleted
                            val intent = Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED)
                            intent.putExtra("expense", expense)
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.TO.value)
                            LocalBroadcastManager.getInstance(fragment.requireContext()).sendBroadcast(intent)
                        }
                        // Delete all
                        5 -> {
                            // Send notification to inform views that this expense has been deleted
                            val intent = Intent(MainActivity.INTENT_RECURRING_EXPENSE_DELETED)
                            intent.putExtra("expense", expense)
                            intent.putExtra("deleteType", RecurringExpenseDeleteType.ALL.value)
                            LocalBroadcastManager.getInstance(fragment.requireContext()).sendBroadcast(intent)
                        }
                    }
                }
                builder.show()
            } else {
                val builder = MaterialAlertDialogBuilder(fragment.requireContext())
                builder.setTitle(if (expense.isRevenue()) R.string.dialog_edit_income_title else R.string.dialog_edit_expense_title)
                builder.setItems(if (expense.isRevenue()) R.array.dialog_edit_income_choices else R.array.dialog_edit_expense_choices) { _, which ->
                    when (which) {
                        0 // Edit expense
                        -> {
                            val startIntent = ExpenseEditActivity.newIntent(
                                context = viewHolder.view.context,
                                editedExpense = expense,
                                date = expense.date,
                            )

                            fragment.requireActivity().startActivity(startIntent)
                        }
                        1 // Delete
                        -> {
                            // Send notification to inform views that this expense has been deleted
                            val intent = Intent(MainActivity.INTENT_EXPENSE_DELETED)
                            intent.putExtra("expense", expense)
                            LocalBroadcastManager.getInstance(fragment.requireContext()).sendBroadcast(intent)
                        }
                    }
                }
                builder.show()
            }

        }

        viewHolder.view.setOnClickListener(onClickListener)

        viewHolder.view.setOnLongClickListener { v ->
            onClickListener.onClick(v)
            true
        }
    }

    override fun getItemCount(): Int = expenses.size

// ------------------------------------------->

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val expenseTitleTextView: TextView = view.findViewById(R.id.expense_title)
        val expenseAmountTextView: TextView = view.findViewById(R.id.expense_amount)
        val recurringIndicator: ViewGroup = view.findViewById(R.id.recurring_indicator)
        val recurringIndicatorTextview: TextView = view.findViewById(R.id.recurring_indicator_textview)
        val positiveIndicator: ImageView = view.findViewById(R.id.positive_indicator)
        val checkedCheckBox: CheckBox = view.findViewById(R.id.expense_checked)
    }
}