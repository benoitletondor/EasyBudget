package com.benoitletondor.easybudgetapp.view.selectcurrency

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SelectCurrencyViewModel : ViewModel() {
    val currenciesLiveData = MutableLiveData<Pair<List<Currency>, List<Currency>>>()

    init {
        viewModelScope.launch {
            val data = withContext(Dispatchers.Default) {
                Pair(CurrencyHelper.getMainAvailableCurrencies(), CurrencyHelper.getOtherAvailableCurrencies())
            }

            currenciesLiveData.value = data
        }
    }
}