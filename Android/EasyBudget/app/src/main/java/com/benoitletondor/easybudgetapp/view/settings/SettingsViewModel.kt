package com.benoitletondor.easybudgetapp.view.settings

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel(assistedFactory = SettingsViewModelFactory::class)
class SettingsViewModel @Inject constructor(
    @Assisted private val redirectToBackupSettings: Boolean,
) : ViewModel() {

}

@AssistedFactory
interface SettingsViewModelFactory {
    fun create(redirectToBackupSettings: Boolean): SettingsViewModel
}