/*
 *   Copyright 2019 Benoit LETONDOR
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.view.DatePickerDialogFragment
import kotlinx.android.synthetic.main.activity_recurring_expense_add.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class RecurringExpenseAddActivity : AppCompatActivity() {
    private val parameters: Parameters by inject()
    private val viewModel: RecurringExpenseAddViewModel by viewModel()

// ------------------------------------------->

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_expense_add)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            viewModel.initWithDateAndExpense(Date(intent.getLongExtra("dateStart", 0)))
            setUpInputs()
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

        viewModel.editTypeIsRevenueLiveData.observe(this, Observer { isRevenue ->
            setExpenseTypeTextViewLayout(isRevenue)
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
                val value = java.lang.Double.parseDouble(amount_edittext.text.toString())

                viewModel.onSave(value, description_edittext.text.toString(), getRecurringTypeFromSpinnerSelection(expense_type_spinner.selectedItemPosition))
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean) {
        if (isRevenue) {
            expense_type_tv.setText(R.string.income)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            expense_type_switch.isChecked = true

            setTitle(R.string.title_activity_recurring_income_add)
        } else {
            expense_type_tv.setText(R.string.payment)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            expense_type_switch.isChecked = false

            setTitle(R.string.title_activity_recurring_expense_add)
        }
    }

    /**
     * Set up text fields, spinner and focus behavior
     */
    private fun setUpInputs() {
        amount_inputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

        amount_edittext.preventUnsupportedInputForDecimals()

        val recurringTypesString = arrayOfNulls<String>(7)
        recurringTypesString[0] = getString(R.string.recurring_interval_daily)
        recurringTypesString[1] = getString(R.string.recurring_interval_weekly)
        recurringTypesString[2] = getString(R.string.recurring_interval_bi_weekly)
        recurringTypesString[3] = getString(R.string.recurring_interval_ter_weekly)
        recurringTypesString[4] = getString(R.string.recurring_interval_four_weekly)
        recurringTypesString[5] = getString(R.string.recurring_interval_monthly)
        recurringTypesString[6] = getString(R.string.recurring_interval_yearly)

        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, recurringTypesString)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        expense_type_spinner.adapter = adapter

        expense_type_spinner.setSelection(5, false)
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
            6 -> return RecurringExpenseType.YEARLY
        }

        throw IllegalStateException("getRecurringTypeFromSpinnerSelection unable to get value for $spinnerSelectedItem")
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

}