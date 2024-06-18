package com.benoitletondor.easybudgetapp.view.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.AppWithTopAppBarScaffold
import com.benoitletondor.easybudgetapp.compose.BackButtonBehavior
import kotlinx.serialization.Serializable

@Serializable
data class SettingsViewDestination(val redirectToBackupSettings: Boolean)

@Composable
fun SettingsView(
    viewModel: SettingsViewModel,
    navigateUp: () -> Unit,
) {
    SettingsView(
        navigateUp = navigateUp,
    )
}

@Composable
private fun SettingsView(
    navigateUp: () -> Unit,
) {
    AppWithTopAppBarScaffold(
        title = stringResource(R.string.title_activity_settings),
        backButtonBehavior = BackButtonBehavior.NavigateBack(
            onBackButtonPressed = navigateUp,
        ),
        content = { contentPadding ->
            // TODO
        }
    )
}