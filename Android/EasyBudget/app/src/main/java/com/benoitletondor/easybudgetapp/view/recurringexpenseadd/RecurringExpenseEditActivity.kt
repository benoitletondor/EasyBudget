/*
 *   Copyright 2021 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.view.recurringexpenseadd

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.view.DatePickerDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_recurring_expense_add.*
import kotlinx.android.synthetic.main.activity_recurring_expense_add.amount_edittext
import kotlinx.android.synthetic.main.activity_recurring_expense_add.amount_inputlayout
import kotlinx.android.synthetic.main.activity_recurring_expense_add.date_button
import kotlinx.android.synthetic.main.activity_recurring_expense_add.description_edittext
import kotlinx.android.synthetic.main.activity_recurring_expense_add.expense_type_switch
import kotlinx.android.synthetic.main.activity_recurring_expense_add.expense_type_tv
import kotlinx.android.synthetic.main.activity_recurring_expense_add.save_expense_fab
import kotlinx.android.synthetic.main.activity_recurring_expense_add.toolbar
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class RecurringExpenseEditActivity : BaseActivity() {
    private val viewModel: RecurringExpenseEditViewModel by viewModels()

    @Inject lateinit var parameters: Parameters

// ------------------------------------------->

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_expense_add)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            viewModel.initWithDateAndExpense(Date(intent.getLongExtra("dateStart", 0)), intent.getParcelableExtra("expense"))
        }

        setUpButtons()

        setResult(Activity.RESULT_CANCELED)

        if ( willAnimateActivityEnter() ) {
            animateActivityEnter(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    description_edittext.setFocus()
                    save_expense_fab.animateFABAppearance()
                }
            })
        } else {
            description_edittext.setFocus()
            save_expense_fab.animateFABAppearance()
        }

        date_button.removeButtonBorder() // Remove border

        viewModel.editTypeLiveData.observe(this, Observer { (isRevenue, isEditing) ->
            setExpenseTypeTextViewLayout(isRevenue, isEditing)
        })

        viewModel.existingExpenseEventStream.observe(this, Observer { existingValues ->
            if( existingValues != null ) {
                setUpTextFields(existingValues.title, existingValues.amount, type = existingValues.type)
            } else {
                setUpTextFields(description = null, amount = null, type = null)
            }
        })

        viewModel.expenseDateLiveData.observe(this, Observer { date ->
            setUpDateButton(date)
        })

        var progressDialog: ProgressDialog? = null
        viewModel.savingIsRevenueEventStream.observe(this, Observer { isRevenue ->
            // Show a ProgressDialog
            val dialog = ProgressDialog(this)
            dialog.isIndeterminate = true
            dialog.setTitle(R.string.recurring_expense_add_loading_title)
            dialog.setMessage(getString(if (isRevenue ) R.string.recurring_income_add_loading_message else R.string.recurring_expense_add_loading_message))
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()

            progressDialog = dialog
        })

        viewModel.finishLiveData.observe(this, Observer {
            progressDialog?.dismiss()
            progressDialog = null

            setResult(Activity.RESULT_OK)
            finish()
        })

        viewModel.errorEventStream.observe(this, Observer {
            progressDialog?.dismiss()
            progressDialog = null

            AlertDialog.Builder(this)
                .setTitle(R.string.recurring_expense_add_error_title)
                .setMessage(getString(R.string.recurring_expense_add_error_message))
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        })

        viewModel.expenseAddBeforeInitDateEventStream.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(getCurrentAmount(), description_edittext.text.toString(), getRecurringTypeFromSpinnerSelection(expense_type_spinner.selectedItemPosition))
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }
                .show()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

// ----------------------------------->

    /**
     * Validate user inputs
     *
     * @return true if user inputs are ok, false otherwise
     */
    private fun validateInputs(): Boolean {
        var ok = true

        val description = description_edittext.text.toString()
        if (description.trim { it <= ' ' }.isEmpty()) {
            description_edittext.error = resources.getString(R.string.no_description_error)
            ok = false
        }

        val amount = amount_edittext.text.toString()
        if (amount.trim { it <= ' ' }.isEmpty()) {
            amount_edittext.error = resources.getString(R.string.no_amount_error)
            ok = false
        } else {
            try {
                val value = java.lang.Double.parseDouble(amount)
                if (value <= 0) {
                    amount_edittext.error = resources.getString(R.string.negative_amount_error)
                    ok = false
                }
            } catch (e: Exception) {
                amount_edittext.error = resources.getString(R.string.invalid_amount)
                ok = false
            }

        }

        return ok
    }

    /**
     * Set-up revenue and payment buttons
     */
    private fun setUpButtons() {
        expense_type_switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onExpenseRevenueValueChanged(isChecked)
        }

        expense_type_tv.setOnClickListener {
            viewModel.onExpenseRevenueValueChanged(!expense_type_switch.isChecked)
        }

        save_expense_fab.setOnClickListener {
            if (validateInputs()) {
                viewModel.onSave(getCurrentAmount(), description_edittext.text.toString(), getRecurringTypeFromSpinnerSelection(expense_type_spinner.selectedItemPosition))
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEditing: Boolean) {
        if (isRevenue) {
            expense_type_tv.setText(R.string.income)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            expense_type_switch.isChecked = true

            if( isEditing ) {
                setTitle(R.string.title_activity_recurring_income_edit)
            } else {
                setTitle(R.string.title_activity_recurring_income_add)
            }
        } else {
            expense_type_tv.setText(R.string.payment)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            expense_type_switch.isChecked = false

            if( isEditing ) {
                setTitle(R.string.title_activity_recurring_expense_edit)
            } else {
                setTitle(R.string.title_activity_recurring_expense_add)
            }
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(description: String?, amount: Double?, type: RecurringExpenseType?) {
        amount_inputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

        val recurringTypesString = arrayOf(
            getString(R.string.recurring_interval_daily),
            getString(R.string.recurring_interval_weekly),
            getString(R.string.recurring_interval_bi_weekly),
            getString(R.string.recurring_interval_ter_weekly),
            getString(R.string.recurring_interval_four_weekly),
            getString(R.string.recurring_interval_monthly),
            getString(R.string.recurring_interval_bi_monthly),
            getString(R.string.recurring_interval_ter_monthly),
            getString(R.string.recurring_interval_six_monthly),
            getString(R.string.recurring_interval_yearly)
        )

        val adapter = ArrayAdapter(this, R.layout.spinner_item, recurringTypesString)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        expense_type_spinner.adapter = adapter

        if( type != null ) {
            setSpinnerSelectionFromRecurringType(type)
        } else {
            setSpinnerSelectionFromRecurringType(RecurringExpenseType.MONTHLY)
        }

        if (description != null) {
            description_edittext.setText(description)
            description_edittext.setSelection(description_edittext.text?.length ?: 0) // Put focus at the end of the text
        }

        amount_edittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            amount_edittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }
    }

    /**
     * Get the recurring expense type associated with the spinner selection
     *
     * @param spinnerSelectedItem index of the spinner selection
     * @return the corresponding expense type
     */
    private fun getRecurringTypeFromSpinnerSelection(spinnerSelectedItem: Int): RecurringExpenseType {
        when (spinnerSelectedItem) {
            0 -> return RecurringExpenseType.DAILY
            1-> return RecurringExpenseType.WEEKLY
            2 -> return RecurringExpenseType.BI_WEEKLY
            3 -> return RecurringExpenseType.TER_WEEKLY
            4 -> return RecurringExpenseType.FOUR_WEEKLY
            5 -> return RecurringExpenseType.MONTHLY
            6 -> return RecurringExpenseType.BI_MONTHLY
            7 -> return RecurringExpenseType.TER_MONTHLY
            8 -> return RecurringExpenseType.SIX_MONTHLY
            9 -> return RecurringExpenseType.YEARLY
        }

        throw IllegalStateException("getRecurringTypeFromSpinnerSelection unable to get value for $spinnerSelectedItem")
    }

    private fun setSpinnerSelectionFromRecurringType(type: RecurringExpenseType) {
        val selectionIndex = when (type) {
            RecurringExpenseType.DAILY -> 0
            RecurringExpenseType.WEEKLY -> 1
            RecurringExpenseType.BI_WEEKLY -> 2
            RecurringExpenseType.TER_WEEKLY -> 3
            RecurringExpenseType.FOUR_WEEKLY -> 4
            RecurringExpenseType.MONTHLY -> 5
            RecurringExpenseType.BI_MONTHLY -> 6
            RecurringExpenseType.TER_MONTHLY -> 7
            RecurringExpenseType.SIX_MONTHLY -> 8
            RecurringExpenseType.YEARLY -> 9
        }

        expense_type_spinner.setSelection(selectionIndex, false)
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: Date) {
        val formatter = SimpleDateFormat(resources.getString(R.string.add_expense_date_format), Locale.getDefault())
        date_button.text = formatter.format(date)

        date_button.setOnClickListener {
            val fragment = DatePickerDialogFragment(date, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val cal = Calendar.getInstance()

                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.onDateChanged(cal.time)
            })

            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(amount_edittext.text.toString())
    }

}