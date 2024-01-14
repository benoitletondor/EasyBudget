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

package com.benoitletondor.easybudgetapp.view.expenseedit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.ActivityExpenseEditBinding
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.view.DatePickerDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

/**
 * Activity to add a new expense
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class ExpenseEditActivity : BaseActivity<ActivityExpenseEditBinding>() {
    private val viewModel: ExpenseEditViewModel by viewModels()

    @Inject lateinit var parameters: Parameters

// -------------------------------------->

    override fun createBinding(): ActivityExpenseEditBinding = ActivityExpenseEditBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val existingExpenseData = viewModel.existingExpenseData
        if (existingExpenseData != null) {
            setUpTextFields(existingExpenseData.title, existingExpenseData.amount)
        } else {
            setUpTextFields(description = null, amount = null)
        }

        setUpButtons()


        binding.descriptionEdittext.setFocus()
        binding.saveExpenseFab.animateFABAppearance()

        lifecycleScope.launchCollect(viewModel.editTypeFlow) { (isRevenue, isEdit) ->
            setExpenseTypeTextViewLayout(isRevenue, isEdit)
        }

        lifecycleScope.launchCollect(viewModel.expenseDateFlow) { date ->
            setUpDateButton(date)
        }

        lifecycleScope.launchCollect(viewModel.unableToLoadDBEventFlow) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.expense_edit_unable_to_load_db_error_title)
                .setMessage(R.string.expense_edit_unable_to_load_db_error_message)
                .setPositiveButton(R.string.expense_edit_unable_to_load_db_error_cta) { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }

        lifecycleScope.launchCollect(viewModel.finishFlow) {
            finish()
        }

        lifecycleScope.launchCollect(viewModel.expenseAddBeforeInitDateEventFlow) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(
                        getCurrentAmount(),
                        binding.descriptionEdittext.text.toString()
                    )
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }
                .show()
        }
    }

// ----------------------------------->

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

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
                val value = java.lang.Double.valueOf(amount)
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
                viewModel.onSave(getCurrentAmount(), binding.descriptionEdittext.text.toString())
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEdit: Boolean) {
        if (isRevenue) {
            binding.expenseTypeTv.setText(R.string.income)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            binding.expenseTypeSwitch.isChecked = true

            setTitle(if (isEdit) R.string.title_activity_edit_income else R.string.title_activity_add_income)
        } else {
            binding.expenseTypeTv.setText(R.string.payment)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            binding.expenseTypeSwitch.isChecked = false

            setTitle(if (isEdit) R.string.title_activity_edit_expense else R.string.title_activity_add_expense)
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(description: String?, amount: Double?) {
        binding.amountInputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

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
     * Set up the date button
     */
    private fun setUpDateButton(date: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern(resources.getString(R.string.add_expense_date_format), Locale.getDefault())
        binding.dateButton.text = formatter.format(date)

        binding.dateButton.setOnClickListener {
            val fragment = DatePickerDialogFragment(date) { _, year, monthOfYear, dayOfMonth ->
                viewModel.onDateChanged(LocalDate.of(year, monthOfYear + 1, dayOfMonth))
            }

            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(binding.amountEdittext.text.toString())
    }

    companion object {
        const val ARG_EDITED_EXPENSE = "expense"
        const val ARG_DATE = "date"

        fun newIntent(
            context: Context,
            editedExpense: Expense?,
            date: LocalDate,
        ): Intent {
            return Intent(context, ExpenseEditActivity::class.java).apply {
                putExtra(ARG_DATE, date.toEpochDay())
                if (editedExpense != null) {
                    putExtra(ARG_EDITED_EXPENSE, editedExpense)
                }
            }
        }
    }
}
