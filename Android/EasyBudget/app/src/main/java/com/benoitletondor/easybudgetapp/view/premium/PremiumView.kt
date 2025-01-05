/*
 *   Copyright 2025 Benoit Letondor
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.easyBudgetGreenColor
import com.benoitletondor.easybudgetapp.compose.rememberPermissionStateCompat
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.iab.PurchaseFlowResult
import com.benoitletondor.easybudgetapp.view.premium.view.ErrorView
import com.benoitletondor.easybudgetapp.view.premium.view.LoadingView
import com.benoitletondor.easybudgetapp.view.premium.view.SubscribeView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class PremiumDestination(val startOnPro: Boolean)

@Composable
fun PremiumView(
    viewModel: PremiumViewModel = hiltViewModel(),
    startOnPro: Boolean,
    close: () -> Unit,
) {
    val context = LocalContext.current

    PremiumView(
        startOnPro = startOnPro,
        eventFlow = viewModel.eventFlow,
        userSubscriptionStatusFlow = viewModel.userSubscriptionStatusFlow,
        onCancelButtonClicked = viewModel::onCancelButtonClicked,
        onBuyPremiumClicked = {
            (context as? Activity)?.let {
                viewModel.onBuyPremiumClicked(it)
            }
        },
        onBuyProClicked = {
            (context as? Activity)?.let {
                viewModel.onBuyProClicked(it)
            }
        },
        onRetryButtonPressed = viewModel::onRetryButtonPressed,
        onCloseButtonPressed = viewModel::onCloseButtonPressed,
        onPushPermissionResult = viewModel::onPushPermissionResult,
        close = close,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PremiumView(
    startOnPro: Boolean,
    eventFlow: Flow<PremiumViewModel.Event>,
    userSubscriptionStatusFlow: StateFlow<PremiumViewModel.SubscriptionStatus>,
    onCancelButtonClicked: () -> Unit,
    onBuyPremiumClicked: () -> Unit,
    onBuyProClicked: () -> Unit,
    onRetryButtonPressed: () -> Unit,
    onCloseButtonPressed: () -> Unit,
    onPushPermissionResult: () -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current

    val pushPermissionState = rememberPermissionStateCompat {
        onPushPermissionResult()
    }

    LaunchedEffect("eventsListener") {
        launchCollect(eventFlow) { event ->
            when(event) {
                PremiumViewModel.Event.Finish -> close()
                is PremiumViewModel.Event.PremiumPurchaseResult -> when(event.result) {
                    PurchaseFlowResult.Cancelled -> Unit
                    is PurchaseFlowResult.Error -> MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(context.getString(R.string.iab_purchase_error_message, event.result.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    is PurchaseFlowResult.Success -> {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.iab_purchase_success_title)
                            .setMessage(R.string.iab_purchase_success_message)
                            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .setOnDismissListener {
                                if (pushPermissionState.status.isGranted) {
                                    onPushPermissionResult()
                                } else {
                                    pushPermissionState.launchPermissionRequest()
                                }
                            }
                            .show()
                    }
                }
                is PremiumViewModel.Event.ProPurchaseResult -> when(event.result) {
                    PurchaseFlowResult.Cancelled -> Unit
                    is PurchaseFlowResult.Error ->  MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(context.getString(R.string.iab_purchase_error_message, event.result.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    is PurchaseFlowResult.Success -> {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.iab_purchase_success_title)
                            .setMessage(R.string.iab_purchase_success_message)
                            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .setOnDismissListener {
                                if (pushPermissionState.status.isGranted) {
                                    onPushPermissionResult()
                                } else {
                                    pushPermissionState.launchPermissionRequest()
                                }
                            }
                            .show()
                    }
                }
            }
        }
    }

    Scaffold(
        content = { contentPaddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .background(easyBudgetGreenColor)
                .padding(contentPaddingValues)
            ) {
                val state by userSubscriptionStatusFlow.collectAsState()

                when (val currentState = state) {
                    is PremiumViewModel.WithPricing -> SubscribeView(
                        currentState.pricing,
                        showProByDefault = startOnPro,
                        premiumSubscribed = currentState is PremiumViewModel.SubscriptionStatus.PremiumSubscribed || currentState is PremiumViewModel.SubscriptionStatus.ProSubscribed,
                        proSubscribed = currentState is PremiumViewModel.SubscriptionStatus.ProSubscribed,
                        onCancelButtonClicked = onCancelButtonClicked,
                        onBuyPremiumButtonClicked = onBuyPremiumClicked,
                        onBuyProButtonClicked = onBuyProClicked,
                    )

                    PremiumViewModel.SubscriptionStatus.Verifying -> LoadingView()
                    PremiumViewModel.SubscriptionStatus.Error -> ErrorView(
                        onRetryButtonPressed = onRetryButtonPressed,
                        onCloseButtonPressed = onCloseButtonPressed,
                    )
                }
            }
        }
    )
}