package com.benoitletondor.easybudgetapp.view.premium2

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
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.setNavigationBarColor
import com.benoitletondor.easybudgetapp.helper.setStatusBarColor
import com.benoitletondor.easybudgetapp.theme.AppTheme
import com.benoitletondor.easybudgetapp.theme.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.view.premium2.view.LoadingView
import com.benoitletondor.easybudgetapp.view.premium2.view.SubscribeView
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

                    when(state) {
                        PremiumViewModel.SubscriptionStatus.Error,
                        PremiumViewModel.SubscriptionStatus.NotSubscribed -> SubscribeView(viewModel, premiumSubscribed = false, proSubscribed = false)
                        PremiumViewModel.SubscriptionStatus.PremiumSubscribed -> SubscribeView(viewModel, premiumSubscribed = true, proSubscribed = false)
                        PremiumViewModel.SubscriptionStatus.ProSubscribed -> SubscribeView(viewModel, premiumSubscribed = true, proSubscribed = true)
                        PremiumViewModel.SubscriptionStatus.Verifying -> LoadingView()
                    }
                }
            }
        }

        setStatusBarColor(R.color.easy_budget_green)
        setNavigationBarColor(R.color.easy_budget_green)
    }
}