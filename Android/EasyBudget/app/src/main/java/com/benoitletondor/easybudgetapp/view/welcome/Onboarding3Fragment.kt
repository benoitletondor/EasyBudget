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

package com.benoitletondor.easybudgetapp.view.welcome

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.databinding.FragmentOnboarding3Binding
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.*
import com.benoitletondor.easybudgetapp.model.Expense
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * Onboarding step 3 fragment
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class Onboarding3Fragment : OnboardingFragment<FragmentOnboarding3Binding>() {
    @Inject lateinit var parameters: Parameters
    @Inject lateinit var db: DB

    override val statusBarColor: Int
        get() = R.color.secondary_dark

    private val amountValue: Double
        get() {
            val valueString = binding?.onboardingScreen3InitialAmountEt?.text.toString()

            return try {
                if ( "" == valueString || "-" == valueString) 0.0 else java.lang.Double.valueOf(valueString)
            } catch (e: Exception) {
                val context = context ?: return 0.0

                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.adjust_balance_error_title)
                    .setMessage(R.string.adjust_balance_error_message)
                    .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()

                Logger.warning("An error occurred during initial amount parsing: $valueString", e)
                return 0.0
            }

        }

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOnboarding3Binding = FragmentOnboarding3Binding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleScope.launch {
            val amount = withContext(Dispatchers.Default) {
                -db.getBalanceForDay(LocalDate.now())
            }

            binding?.onboardingScreen3InitialAmountEt?.setText(if (amount == 0.0) "0" else amount.toString())
        }

        setCurrency()

        binding?.onboardingScreen3InitialAmountEt?.preventUnsupportedInputForDecimals()
        binding?.onboardingScreen3InitialAmountEt?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                setButtonText()
            }
        })

        binding?.onboardingScreen3NextButton?.setOnClickListener { button ->
            viewLifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val currentBalance = -db.getBalanceForDay(LocalDate.now())
                    val newBalance = amountValue

                    if (newBalance != currentBalance) {
                        val diff = newBalance - currentBalance

                        val expense = Expense(resources.getString(R.string.adjust_balance_expense_title), -diff, LocalDate.now(), true)
                        db.persistExpense(expense)
                    }
                }

                // Hide keyboard
                try {
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(binding?.onboardingScreen3InitialAmountEt?.windowToken, 0)
                } catch (e: Exception) {
                    Logger.error("Error while hiding keyboard", e)
                }

                next(button)
            }
        }

        setButtonText()
    }

    override fun onResume() {
        super.onResume()

        setCurrency()
        setButtonText()
    }

// -------------------------------------->

    private fun setCurrency() {
        binding?.onboardingScreen3InitialAmountMoneyTv?.text = parameters.getUserCurrency().symbol
    }

    private fun setButtonText() {
        binding?.onboardingScreen3NextButton?.text = getString(R.string.onboarding_screen_3_cta, CurrencyHelper.getFormattedCurrencyString(parameters, amountValue))
    }
}
