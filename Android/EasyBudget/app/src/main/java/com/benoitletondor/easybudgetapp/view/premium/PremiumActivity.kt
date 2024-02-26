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

package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.setNavigationBarColored
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.iab.PurchaseFlowResult
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.view.premium.view.ErrorView
import com.benoitletondor.easybudgetapp.view.premium.view.LoadingView
import com.benoitletondor.easybudgetapp.view.premium.view.SubscribeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {
    private val viewModel: PremiumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        // Cancelled by default
        setResult(Activity.RESULT_CANCELED)

        setContent {
            AppTheme {
                Box(modifier = Modifier
                    .background(easyBudgetGreenColor)
                    .fillMaxWidth()
                    .fillMaxHeight()
                ) {
                    val state by viewModel.userSubscriptionStatus.collectAsState(PremiumViewModel.SubscriptionStatus.Verifying)

                    when (val currentState = state) {
                        is PremiumViewModel.WithPricing -> SubscribeView(
                            currentState.pricing,
                            showProByDefault = intent.getBooleanExtra(EXTRA_SHOW_PRO, false),
                            premiumSubscribed = currentState is PremiumViewModel.SubscriptionStatus.PremiumSubscribed || currentState is PremiumViewModel.SubscriptionStatus.ProSubscribed,
                            proSubscribed = currentState is PremiumViewModel.SubscriptionStatus.ProSubscribed,
                            onCancelButtonClicked = this@PremiumActivity::finish,
                            onBuyPremiumButtonClicked = {
                                viewModel.onBuyPremiumClicked(this@PremiumActivity)
                            },
                            onBuyProButtonClicked = {
                                viewModel.onBuyProClicked(this@PremiumActivity)
                            }
                        )

                        PremiumViewModel.SubscriptionStatus.Verifying -> LoadingView()
                        PremiumViewModel.SubscriptionStatus.Error -> ErrorView(
                            onRetryButtonPressed = viewModel::onRetryButtonPressed,
                            onCloseButtonPressed = viewModel::onCloseButtonPressed,
                        )
                    }
                }
            }
        }

        setStatusBarColor(R.color.easy_budget_green)
        setNavigationBarColored()

        collectViewModelEvents()
    }

    private fun collectViewModelEvents() {
        lifecycleScope.launchCollect(viewModel.premiumPurchaseEventFlow) { purchaseResult ->
            when(purchaseResult) {
                PurchaseFlowResult.Cancelled -> Unit
                is PurchaseFlowResult.Error -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(getString(R.string.iab_purchase_error_message, purchaseResult.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                is PurchaseFlowResult.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        lifecycleScope.launchCollect(viewModel.proPurchaseEventFlow) { purchaseResult ->
            when(purchaseResult) {
                PurchaseFlowResult.Cancelled -> Unit
                is PurchaseFlowResult.Error -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(getString(R.string.iab_purchase_error_message, purchaseResult.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                is PurchaseFlowResult.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        lifecycleScope.launchCollect(viewModel.eventFlow) { event ->
            when(event) {
                PremiumViewModel.Event.Finish -> finish()
            }
        }
    }

    companion object {
        private const val EXTRA_SHOW_PRO = "showPro"

        fun createIntent(activity: Activity, shouldShowProByDefault: Boolean): Intent {
            return Intent(activity, PremiumActivity::class.java).apply {
                putExtra(EXTRA_SHOW_PRO, shouldShowProByDefault)
            }
        }
    }
}