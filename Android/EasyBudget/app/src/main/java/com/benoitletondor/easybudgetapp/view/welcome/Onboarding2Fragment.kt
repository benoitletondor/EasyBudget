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

package com.benoitletondor.easybudgetapp.view.welcome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.helper.getUserCurrency
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment
import kotlinx.android.synthetic.main.fragment_onboarding2.*
import org.koin.android.ext.android.inject
import java.util.*

/**
 * Onboarding step 2 fragment
 *
 * @author Benoit LETONDOR
 */
class Onboarding2Fragment : OnboardingFragment() {
    private lateinit var selectedCurrency: Currency
    private lateinit var receiver: BroadcastReceiver

    private val parameters: Parameters by inject()

    override val statusBarColor: Int
        get() = R.color.secondary_dark

// ------------------------------------->

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedCurrency = parameters.getUserCurrency()
        setNextButtonText()

        val selectCurrencyFragment = SelectCurrencyFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.expense_select_container, selectCurrencyFragment).commit()

        val filter = IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                selectedCurrency = Currency.getInstance(intent.getStringExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA))
                setNextButtonText()
            }
        }

        LocalBroadcastManager.getInstance(view.context).registerReceiver(receiver, filter)

        onboarding_screen2_next_button.setOnClickListener {
            next()
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(receiver)

        super.onDestroyView()
    }

    /**
     * Set the next button text according to the selected currency
     */
    private fun setNextButtonText() {
        onboarding_screen2_next_button?.text = resources.getString(R.string.onboarding_screen_2_cta, selectedCurrency.symbol)
    }
}
