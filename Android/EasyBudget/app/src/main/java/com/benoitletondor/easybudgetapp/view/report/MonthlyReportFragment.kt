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

package com.benoitletondor.easybudgetapp.view.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.viewLifecycleScope
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.YearMonth
import javax.inject.Inject

private const val ARG_MONTH = "arg_month"

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MonthlyReportFragment : Fragment() {
    /**
     * The first day of the month
     */
    private lateinit var month: YearMonth

    private val viewModel: MonthlyReportViewModel by viewModels()
    @Inject lateinit var parameters: Parameters

// ---------------------------------->

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchCollect(viewModel.unableToLoadDBEventFlow) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.monthly_report_unable_to_load_db_error_title)
                .setMessage(R.string.monthly_report_unable_to_load_db_error_message)
                .setPositiveButton(R.string.monthly_report_unable_to_load_db_error_cta) { _, _ ->
                    requireActivity().finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        month = requireArguments().getSerializable(ARG_MONTH) as YearMonth

        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_monthly_report, container, false)

        val progressBar = v.findViewById<ProgressBar>(R.id.monthly_report_fragment_progress_bar)
        val content = v.findViewById<View>(R.id.monthly_report_fragment_content)
        val recyclerView = v.findViewById<RecyclerView>(R.id.monthly_report_fragment_recycler_view)
        val emptyState = v.findViewById<View>(R.id.monthly_report_fragment_empty_state)
        val revenuesAmountTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_revenues_total_tv)
        val expensesAmountTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_expenses_total_tv)
        val balanceTextView = v.findViewById<TextView>(R.id.monthly_report_fragment_balance_tv)

        viewLifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            when(state) {
                MonthlyReportViewModel.MonthlyReportState.Empty -> {
                    progressBar.visibility = View.GONE
                    content.visibility = View.VISIBLE

                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE

                    revenuesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    expensesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    balanceTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, 0.0)
                    balanceTextView.setTextColor(ContextCompat.getColor(balanceTextView.context, R.color.budget_green))
                }
                is MonthlyReportViewModel.MonthlyReportState.Loaded -> {
                    progressBar.visibility = View.GONE
                    content.visibility = View.VISIBLE

                    configureRecyclerView(recyclerView, MonthlyReportRecyclerViewAdapter(state.expenses, state.revenues, parameters))

                    revenuesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, state.revenuesAmount)
                    expensesAmountTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, state.expensesAmount)

                    val balance = state.revenuesAmount - state.expensesAmount
                    balanceTextView.text = CurrencyHelper.getFormattedCurrencyString(parameters, balance)
                    balanceTextView.setTextColor(ContextCompat.getColor(balanceTextView.context, if (balance >= 0) R.color.budget_green else R.color.budget_red))
                }
                MonthlyReportViewModel.MonthlyReportState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    content.visibility = View.GONE
                }
            }
        }

        viewModel.loadDataForMonth(month)

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
        fun newInstance(month: YearMonth): MonthlyReportFragment = MonthlyReportFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_MONTH, month)
            }
        }
    }
}
