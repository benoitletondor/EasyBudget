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

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope

import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment that contains UI for user to chose its currency.<br></br>
 * You should listen to the [.CURRENCY_SELECTED_INTENT] intent to get notified when
 * the selected currency has changed. The newly selected currency ISO code is available in
 * the [.CURRENCY_ISO_EXTRA] string extra.<br></br>
 * <br></br>
 * NB: The setUserCurrency method is automaticaly called by the fragment on selection, y
 * ou don't have to do it yourself.
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class SelectCurrencyFragment : DialogFragment() {
    private val viewModel: SelectCurrencyViewModel by viewModels()

    @Inject lateinit var parameters: Parameters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (showsDialog) {
            return null
        }

        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_select_currency, container, false)

        setupRecyclerView(v)

        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for this fragment
        val v = LayoutInflater.from(activity).inflate(R.layout.fragment_select_currency, null, false)
        setupRecyclerView(v)

        // Put some padding between title and content
        v.setPadding(0, resources.getDimensionPixelSize(R.dimen.select_currency_dialog_padding_top), 0, 0)

        val builder = MaterialAlertDialogBuilder(requireActivity())

        builder.setView(v)
        builder.setTitle(R.string.setting_category_currency_change_dialog_title)

        return builder.create()
    }

// ------------------------------------->

    /**
     * Setup the recycler view
     *
     * @param v inflated view
     */
    private fun setupRecyclerView(v: View) {
        val recyclerView = v.findViewById<RecyclerView>(R.id.select_currency_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(v.context)

        lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            when(state) {
                is SelectCurrencyViewModel.State.Loaded -> {
                    val adapter = SelectCurrencyRecyclerViewAdapter(state.mainCurrencies, state.otherCurrencies, parameters)
                    recyclerView.adapter = adapter

                    if( adapter.selectedCurrencyPosition() > 1 ) {
                        recyclerView.scrollToPosition(adapter.selectedCurrencyPosition()-1)
                    }
                }
                SelectCurrencyViewModel.State.Loading -> Unit // TODO: loading state
            }
        }
    }

    companion object {
        /**
         * Action of the intent broadcasted when selected currency has changed
         */
        const val CURRENCY_SELECTED_INTENT = "currency.selected"
        /**
         * Key to retrieve the newly selected currency ISO code in Intent extras
         */
        const val CURRENCY_ISO_EXTRA = "currency.iso.key"
    }
}
