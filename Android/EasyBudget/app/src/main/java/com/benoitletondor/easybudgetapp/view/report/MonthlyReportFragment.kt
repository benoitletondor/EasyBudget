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

package com.benoitletondor.easybudgetapp.view.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.parameters.Parameters
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

private const val ARG_DATE = "arg_date"

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportFragment : Fragment() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: Date

    private val parameters: Parameters by inject()
    private val viewModel: MonthlyReportViewModel by viewModel()

// ---------------------------------->

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        date = arguments!!.getSerializable(ARG_DATE) as Date

        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_monthly_report, container, false)

        val progressBar = v.findViewById<ProgressBar>(R.id.monthly_report_fragment_progress_bar)
        val content = v.findViewById<View>(R.id.monthly_report_fragment_content)
        val recyclerView = v.findViewById<RecyclerView>(R.id.monthly_report_fragment_recycler_view)
        val emptyState = v.findViewById<View>(R.id.monthly_report_fragment_empty_state)
        val revenuesAmountTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_revenues_total_tv)
        val expensesAmountTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_expenses_total_tv)
        val balanceTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_balance_tv)

        viewModel.monthlyReportDataLiveData.observe(this, Observer { result ->
            progressBar.visibility = View.GONE
            content.visibility = View.VISIBLE

            when(result) {
                MonthlyReportViewModel.MonthlyReportData.Empty -> {
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE

                    revenuesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    expensesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    balanceTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    balanceTextView.setTextColor(ContextCompat.getColor(balanceTextView.context, R.color.budget_green))
                }
                is MonthlyReportViewModel.MonthlyReportData.Data -> {
                    configureRecyclerView(recyclerView, MonthlyReportRecyclerViewAdapter(result.expenses, result.revenues, parameters))

                    revenuesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, result.revenuesAmount)
                    expensesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, result.expensesAmount)

                    val balance = result.revenuesAmount - result.expensesAmount
                    balanceTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, balance)
                    balanceTextView.setTextColor(ContextCompat.getColor(balanceTextView.context, if (balance >= 0) R.color.budget_green else R.color.budget_red))
                }
            }
        })

        viewModel.loadDataForMonth(date)

        return v
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(recyclerView: RecyclerView, adapter: MonthlyReportRecyclerViewAdapter) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: Date): MonthlyReportFragment = MonthlyReportFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
    }
}
