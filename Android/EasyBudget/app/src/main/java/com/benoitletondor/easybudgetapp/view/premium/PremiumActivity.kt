package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
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
import com.benoitletondor.easybudgetapp.helper.setNavigationBarColor
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.iab.PurchaseFlowResult
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.view.premium.view.LoadingView
import com.benoitletondor.easybudgetapp.view.premium.view.SubscribeView
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
                    val premiumBuyingState by viewModel.premiumPurchaseStatusFlow.collectAsState(PremiumViewModel.PurchaseFlowStatus.NOT_STARTED)
                    val proBuyingState by viewModel.proPurchaseStatusFlow.collectAsState(PremiumViewModel.PurchaseFlowStatus.NOT_STARTED)

                    when(state) {
                        PremiumViewModel.SubscriptionStatus.Error,
                        PremiumViewModel.SubscriptionStatus.NotSubscribed,
                        PremiumViewModel.SubscriptionStatus.PremiumSubscribed,
                        PremiumViewModel.SubscriptionStatus.ProSubscribed -> SubscribeView(
                            viewModel,
                            premiumSubscribed = state == PremiumViewModel.SubscriptionStatus.PremiumSubscribed,
                            proSubscribed = state == PremiumViewModel.SubscriptionStatus.ProSubscribed,
                            onCancelButtonClicked = {
                                finish()
                            },
                            onBuyPremiumButtonClicked = {
                                viewModel.onBuyPremiumClicked(this@PremiumActivity)
                            },
                            onBuyProButtonClicked = {
                                viewModel.onBuyProClicked(this@PremiumActivity)
                            }
                        )
                        PremiumViewModel.SubscriptionStatus.Verifying -> LoadingView()
                    }
                }
            }
        }

        setStatusBarColor(R.color.easy_budget_green)
        setNavigationBarColor(R.color.easy_budget_green)

        collectViewModelEvents()
    }

    private fun collectViewModelEvents() {
        lifecycleScope.launchCollect(viewModel.premiumPurchaseEventFlow) { purchaseResult ->
            when(purchaseResult) {
                PurchaseFlowResult.Cancelled -> TODO()
                is PurchaseFlowResult.Error -> TODO()
                is PurchaseFlowResult.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

        lifecycleScope.launchCollect(viewModel.proPurchaseEventFlow) { purchaseResult ->
            when(purchaseResult) {
                PurchaseFlowResult.Cancelled -> TODO()
                is PurchaseFlowResult.Error -> TODO()
                is PurchaseFlowResult.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }
}