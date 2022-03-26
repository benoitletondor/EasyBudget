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
import android.app.ProgressDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityRecurringExpenseAddBinding
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import com.benoitletondor.easybudgetapp.view.DatePickerDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class RecurringExpenseEditActivity : BaseActivity<ActivityRecurringExpenseAddBinding>() {
    private val viewModel: RecurringExpenseEditViewModel by viewModels()

    @Inject lateinit var parameters: Parameters

// ------------------------------------------->

    override fun createBinding(): ActivityRecurringExpenseAddBinding = ActivityRecurringExpenseAddBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)

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
                    binding.descriptionEdittext.setFocus()
                    binding.saveExpenseFab.animateFABAppearance()
                }
            })
        } else {
            binding.descriptionEdittext.setFocus()
            binding.saveExpenseFab.animateFABAppearance()
        }

        binding.dateButton.removeButtonBorder() // Remove border

        viewModel.editTypeLiveData.observe(this) { (isRevenue, isEditing) ->
            setExpenseTypeTextViewLayout(isRevenue, isEditing)
        }

        viewModel.existingExpenseEventStream.observe(this) { existingValues ->
            if (existingValues != null) {
                setUpTextFields(
                    existingValues.title,
                    existingValues.amount,
                    type = existingValues.type
                )
            } else {
                setUpTextFields(description = null, amount = null, type = null)
            }
        }

        viewModel.expenseDateLiveData.observe(this) { date ->
            setUpDateButton(date)
        }

        var progressDialog: ProgressDialog? = null
        viewModel.savingIsRevenueEventStream.observe(this) { isRevenue ->
            // Show a ProgressDialog
            val dialog = ProgressDialog(this)
            dialog.isIndeterminate = true
            dialog.setTitle(R.string.recurring_expense_add_loading_title)
            dialog.setMessage(getString(if (isRevenue) R.string.recurring_income_add_loading_message else R.string.recurring_expense_add_loading_message))
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()

            progressDialog = dialog
        }

        viewModel.finishLiveData.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null

            setResult(Activity.RESULT_OK)
            finish()
        }

        viewModel.errorEventStream.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null

            AlertDialog.Builder(this)
                .setTitle(R.string.recurring_expense_add_error_title)
                .setMessage(getString(R.string.recurring_expense_add_error_message))
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        viewModel.expenseAddBeforeInitDateEventStream.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(
                        getCurrentAmount(),
                        binding.descriptionEdittext.text.toString(),
                        getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition)
                    )
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }
                .show()
        }
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

        val description = binding.descriptionEdittext.text.toString()
        if (description.trim { it <= ' ' }.isEmpty()) {
            binding.descriptionEdittext.error = resources.getString(R.string.no_description_error)
            ok = false
        }

        val amount = binding.amountEdittext.text.toString()
        if (amount.trim { it <= ' ' }.isEmpty()) {
            binding.amountEdittext.error = resources.getString(R.string.no_amount_error)
            ok = false
        } else {
            try {
                val value = java.lang.Double.parseDouble(amount)
                if (value <= 0) {
                    binding.amountEdittext.error = resources.getString(R.string.negative_amount_error)
                    ok = false
                }
            } catch (e: Exception) {
                binding.amountEdittext.error = resources.getString(R.string.invalid_amount)
                ok = false
            }

        }

        return ok
    }

    /**
     * Set-up revenue and payment buttons
     */
    private fun setUpButtons() {
        binding.expenseTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onExpenseRevenueValueChanged(isChecked)
        }

        binding.expenseTypeTv.setOnClickListener {
            viewModel.onExpenseRevenueValueChanged(!binding.expenseTypeSwitch.isChecked)
        }

        binding.saveExpenseFab.setOnClickListener {
            if (validateInputs()) {
                viewModel.onSave(
                    getCurrentAmount(),
                    binding.descriptionEdittext.text.toString(),
                    getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition),
                )
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEditing: Boolean) {
        if (isRevenue) {
            binding.expenseTypeTv.setText(R.string.income)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            binding.expenseTypeSwitch.isChecked = true

            if( isEditing ) {
                setTitle(R.string.title_activity_recurring_income_edit)
            } else {
                setTitle(R.string.title_activity_recurring_income_add)
            }
        } else {
            binding.expenseTypeTv.setText(R.string.payment)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            binding.expenseTypeSwitch.isChecked = false

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
        binding.amountInputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

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
        binding.expenseTypeSpinner.adapter = adapter

        if( type != null ) {
            setSpinnerSelectionFromRecurringType(type)
        } else {
            setSpinnerSelectionFromRecurringType(RecurringExpenseType.MONTHLY)
        }

        if (description != null) {
            binding.descriptionEdittext.setText(description)
            binding.descriptionEdittext.setSelection(binding.descriptionEdittext.text?.length ?: 0) // Put focus at the end of the text
        }

        binding.amountEdittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            binding.amountEdittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
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

        binding.expenseTypeSpinner.setSelection(selectionIndex, false)
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: Date) {
        val formatter = SimpleDateFormat(resources.getString(R.string.add_expense_date_format), Locale.getDefault())
        binding.dateButton.text = formatter.format(date)

        binding.dateButton.setOnClickListener {
            val fragment = DatePickerDialogFragment(date) { _, year, monthOfYear, dayOfMonth ->
                val cal = Calendar.getInstance()

                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.onDateChanged(cal.time)
            }

            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(binding.amountEdittext.text.toString())
    }

}