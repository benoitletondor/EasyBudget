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

package com.benoitletondor.easybudgetapp.view.expenseedit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.helper.UIHelper
import com.benoitletondor.easybudgetapp.helper.getUserCurrency
import com.benoitletondor.easybudgetapp.view.DatePickerDialogFragment
import kotlinx.android.synthetic.main.activity_expense_edit.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Activity to add a new expense
 *
 * @author Benoit LETONDOR
 */
class ExpenseEditActivity : AppCompatActivity() {
    private val parameters: Parameters by inject()
    private val viewModel: ExpenseEditViewModel by viewModel()

    private var isContentScrollable = false

// -------------------------------------->

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_edit)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.existingExpenseEventStream.observe(this, Observer { existingValues ->
            if( existingValues != null ) {
                setUpTextFields(existingValues.title, existingValues.amount)
            } else {
                setUpTextFields(description = null, amount = null)
            }
        })

        if( savedInstanceState == null ) {
            viewModel.initWithDateAndExpense(Date(intent.getLongExtra("date", 0)), intent.getParcelableExtra("expense"))
        }

        setUpButtons()

        setResult(Activity.RESULT_CANCELED)

        if (UIHelper.willAnimateActivityEnter(this)) {
            UIHelper.animateActivityEnter(this, object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    UIHelper.setFocus(description_edittext)
                    UIHelper.showFAB(save_expense_fab)
                }
            })
        } else {
            UIHelper.setFocus(description_edittext)
            UIHelper.showFAB(save_expense_fab)
        }

        UIHelper.removeButtonBorder(date_button) // Remove border on lollipop

        viewModel.editTypeLiveData.observe(this, Observer { (isRevenue, isEdit) ->
            setExpenseTypeTextViewLayout(isRevenue, isEdit)
        })

        viewModel.expenseDateLiveData.observe(this, Observer { date ->
            setUpDateButton(date)
        })

        viewModel.finishEventStream.observe(this, Observer {
            setResult(Activity.RESULT_OK)
            finish()
        })


        // Only show save toolbar item if save button is not fully visible
        expense_scroll_view.viewTreeObserver.addOnGlobalLayoutListener {
            val scrollView = expense_scroll_view ?: return@addOnGlobalLayoutListener
            val childHeight = expense_scroll_content?.height ?: return@addOnGlobalLayoutListener
            val contentScrollable = scrollView.height < childHeight + scrollView.paddingTop + scrollView.paddingBottom
            if( isContentScrollable != contentScrollable ) {
                isContentScrollable = contentScrollable
                invalidateOptionsMenu()
            }
        }
    }

// ----------------------------------->

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_expense, menu)

        if( !isContentScrollable ) {
            menu.removeItem(R.id.action_save)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_save -> {
                if (validateInputs()) {
                    val value = java.lang.Double.parseDouble(amount_edittext.text.toString())

                    viewModel.onSave(value, description_edittext.text.toString())
                }

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
                val value = java.lang.Double.valueOf(amount)
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

                viewModel.onSave(value, description_edittext.text.toString())
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEdit: Boolean) {
        if (isRevenue) {
            expense_type_tv.setText(R.string.income)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            expense_type_switch.isChecked = true

            setTitle(if (isEdit) R.string.title_activity_edit_income else R.string.title_activity_add_income)
        } else {
            expense_type_tv.setText(R.string.payment)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            expense_type_switch.isChecked = false

            setTitle(if (isEdit) R.string.title_activity_edit_expense else R.string.title_activity_add_expense)
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(description: String?, amount: Double?) {
        amount_inputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

        if (description != null) {
            description_edittext.setText(description)
            description_edittext.setSelection(description_edittext.text?.length ?: 0) // Put focus at the end of the text
        }

        UIHelper.preventUnsupportedInputForDecimals(amount_edittext)

        if (amount != null) {
            amount_edittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: Date) {
        val formatter = SimpleDateFormat(resources.getString(R.string.add_expense_date_format), Locale.getDefault())
        date_button.text = formatter.format(date)

        date_button.setOnClickListener {
            val fragment = DatePickerDialogFragment(date, DatePickerDialog.OnDateSetListener {  _, year, monthOfYear, dayOfMonth ->
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
