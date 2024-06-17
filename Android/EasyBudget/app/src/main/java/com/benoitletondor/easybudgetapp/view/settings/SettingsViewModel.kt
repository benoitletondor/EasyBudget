package com.benoitletondor.easybudgetapp.view.settings

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = SettingsViewModelFactory::class)
class SettingsViewModel @AssistedInject constructor(
    @Assisted private val redirectToBackupSettings: Boolean,
) : ViewModel() {

}

@AssistedFactory
interface SettingsViewModelFactory {
    fun create(redirectToBackupSettings: Boolean): SettingsViewModel
}