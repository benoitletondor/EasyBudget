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

package com.benoitletondor.easybudgetapp.view.selectcurrency

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.helper.getUserCurrency
import com.benoitletondor.easybudgetapp.helper.setUserCurrency

import java.util.Currency

/**
 * View adapter for the Recycler view of the [SelectCurrencyFragment]
 *
 * @author Benoit LETONDOR
 */
class SelectCurrencyRecyclerViewAdapter(private val mainCurrencies: List<Currency>,
                                        private val secondaryCurrencies: List<Currency>,
                                        private val parameters: Parameters) : RecyclerView.Adapter<SelectCurrencyRecyclerViewAdapter.ViewHolder>() {

    /**
     * Get the position of the selected currency
     */
    fun selectedCurrencyPosition(): Int {
        val currency = parameters.getUserCurrency()

        return if (mainCurrencies.contains(currency)) mainCurrencies.indexOf(currency) else secondaryCurrencies.indexOf(currency) + 1 + mainCurrencies.size
    }

// ---------------------------------------->

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == TYPE_MAIN_CURRENCY || viewType == TYPE_SECONDARY_CURRENCY) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.recycleview_currency_cell, parent, false)
            ViewHolder(v, viewType)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.recycleview_currency_separator_cell, parent, false)
            ViewHolder(v, viewType, true)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!holder.separator) {
            val currency = if (holder.type == TYPE_MAIN_CURRENCY) mainCurrencies[position] else secondaryCurrencies[position - 1 - mainCurrencies.size]

            val userCurrency = parameters.getUserCurrency() == currency

            holder.selectedIndicator?.visibility = if (userCurrency) View.VISIBLE else View.INVISIBLE
            holder.currencyTitle?.text = CurrencyHelper.getCurrencyDisplayName(currency)
            holder.v.setOnClickListener { v ->
                // Set the currency
                parameters.setUserCurrency(currency)
                // Reload date to change the checkmark
                notifyDataSetChanged()

                // Broadcast the intent
                val intent = Intent(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
                intent.putExtra(SelectCurrencyFragment.CURRENCY_ISO_EXTRA, currency.currencyCode)

                LocalBroadcastManager.getInstance(v.context).sendBroadcast(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return mainCurrencies.size + 1 + secondaryCurrencies.size
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < mainCurrencies.size -> TYPE_MAIN_CURRENCY
            position == mainCurrencies.size -> TYPE_SEPARATOR
            else -> TYPE_SECONDARY_CURRENCY
        }
    }

// ------------------------------------------->

    class ViewHolder(val v: View, val type: Int, val separator: Boolean = false) : RecyclerView.ViewHolder(v) {
        var currencyTitle: TextView? = null
        var selectedIndicator: ImageView? = null

        init {
            if (!separator) {
                currencyTitle = v.findViewById(R.id.currency_cell_title_tv)
                selectedIndicator = v.findViewById(R.id.currency_cell_selected_indicator_iv)
            }
        }
    }

    companion object {
        private const val TYPE_MAIN_CURRENCY = 0
        private const val TYPE_SEPARATOR = 1
        private const val TYPE_SECONDARY_CURRENCY = 2
    }
}
