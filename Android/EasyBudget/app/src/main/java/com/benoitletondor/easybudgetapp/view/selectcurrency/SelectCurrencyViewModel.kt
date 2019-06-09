package com.benoitletondor.easybudgetapp.view.selectcurrency

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SelectCurrencyViewModel : ViewModel() {
    val currenciesLiveData = MutableLiveData<Pair<List<Currency>, List<Currency>>>()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val availableCurrencies = CurrencyHelper.getMainAvailableCurrencies()
            val otherAvailableCurrencies = CurrencyHelper.getOtherAvailableCurrencies()

            currenciesLiveData.postValue(Pair(availableCurrencies, otherAvailableCurrencies))
        }
    }
}