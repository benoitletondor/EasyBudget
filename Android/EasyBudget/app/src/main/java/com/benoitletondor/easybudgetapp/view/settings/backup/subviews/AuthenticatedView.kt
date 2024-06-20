package com.benoitletondor.easybudgetapp.view.settings.backup.subviews

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.view.settings.backup.BackupSettingsViewModel

@Composable
fun ColumnScope.AuthenticatedView(
    state: BackupSettingsViewModel.State.Authenticated,
    onLogoutButtonClicked: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.backup_settings_your_google_account),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = state.currentUser.email,
        fontSize = 16.sp,
    )

    TextButton(
        modifier = Modifier.align(Alignment.End),
        onClick = onLogoutButtonClicked,
    ) {
        Text(text = stringResource(R.string.backup_settings_logout_cta))
    }

    Spacer(modifier = Modifier.height(30.dp))

    when(state) {
        is BackupSettingsViewModel.State.Activated -> TODO()
        is BackupSettingsViewModel.State.NotActivated -> TODO()
        is BackupSettingsViewModel.State.BackupInProgress -> TODO()
        is BackupSettingsViewModel.State.DeletionInProgress -> TODO()
        is BackupSettingsViewModel.State.RestorationInProgress -> TODO()
    }
}