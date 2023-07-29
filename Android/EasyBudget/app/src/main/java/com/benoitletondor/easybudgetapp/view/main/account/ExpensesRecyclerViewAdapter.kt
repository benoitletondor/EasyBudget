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

package com.benoitletondor.easybudgetapp.view.main.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.model.RecurringExpenseDeleteType
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.expenseedit.ExpenseEditActivity
import com.benoitletondor.easybudgetapp.view.main.MainActivity
import com.benoitletondor.easybudgetapp.view.main.MainViewModel
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
    private val account: MainViewModel.SelectedAccount.Selected,
    private val parameters: Parameters,
    private var date: LocalDate,
    private val onExpenseCheckedListener: (Expense, Boolean) -> Unit,
) : RecyclerView.Adapter<ExpensesRecyclerViewAdapter.ViewHolder>() {

    private var expenses = mutableListOf<Expense>()
    private var isUserPremium = false

    fun getDate(): LocalDate = date

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

    /**
     * Remove given expense
     *
     * @param expense the expense to remove
     * @return position of the deleted expense (-1 if not found)
     */
    fun removeExpense(expense: Expense): Int {
        val expenseIterator = expenses.iterator()
        var position = 0
        while (expenseIterator.hasNext()) {
            val (id) = expenseIterator.next()
            if (expense.id != null && expense.id == id) {
                expenseIterator.remove()
                notifyItemRemoved(position)
                return position
            }

            position++
        }

        return -1
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
            when (expense.associatedRecurringExpense!!.recurringExpense.type) {
                RecurringExpenseType.DAILY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.daily)
                RecurringExpenseType.WEEKLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.weekly)
                RecurringExpenseType.BI_WEEKLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.bi_weekly)
                RecurringExpenseType.TER_WEEKLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.ter_weekly)
                RecurringExpenseType.FOUR_WEEKLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.four_weekly)
                RecurringExpenseType.MONTHLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.monthly)
                RecurringExpenseType.BI_MONTHLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.bi_monthly)
                RecurringExpenseType.TER_MONTHLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.ter_monthly)
                RecurringExpenseType.SIX_MONTHLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.six_monthly)
                RecurringExpenseType.YEARLY -> viewHolder.recurringIndicatorTextview.text = viewHolder.view.context.getString(R.string.yearly)
            }
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
                                account = account,
                                editedExpense = expense,
                                date = expense.date,
                            )

                            ActivityCompat.startActivityForResult(fragment.requireActivity(), startIntent,
                                MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null)
                        }
                        // Edit this one and following ones
                        1 -> {
                            val startIntent = ExpenseEditActivity.newIntent(
                                context = viewHolder.view.context,
                                account = account,
                                editedExpense = expense,
                                date = expense.date,
                            )

                            ActivityCompat.startActivityForResult(fragment.requireActivity(), startIntent,
                                MainActivity.MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE, null)
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
                                account = account,
                                editedExpense = expense,
                                date = expense.date,
                            )

                            ActivityCompat.startActivityForResult(fragment.requireActivity(), startIntent,
                                MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null)
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