package com.benoitletondor.easybudgetapp.view.premium

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.SingleLiveEvent
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumPurchaseFlowResult
import kotlinx.coroutines.launch

class PremiumViewModel(private val iab: Iab) : ViewModel() {
    val premiumFlowStatusLiveData = MutableLiveData(PremiumFlowStatus.NOT_STARTED)
    val premiumFlowErrorEventStream = SingleLiveEvent<PremiumPurchaseFlowResult>()

    fun onBuyPremiumClicked(activity: Activity) {
        premiumFlowStatusLiveData.value = PremiumFlowStatus.LOADING

        viewModelScope.launch {
            when(val result = iab.launchPremiumPurchaseFlow(activity)) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
                PremiumPurchaseFlowResult.Success -> {
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.DONE
                }
                is PremiumPurchaseFlowResult.Error -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
            }
        }
    }
}

enum class PremiumFlowStatus {
    NOT_STARTED,
    LOADING,
    DONE
}