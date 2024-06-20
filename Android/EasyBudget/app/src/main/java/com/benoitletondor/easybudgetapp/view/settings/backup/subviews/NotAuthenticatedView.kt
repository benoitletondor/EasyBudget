package com.benoitletondor.easybudgetapp.view.settings.backup.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R

@Composable
fun NotAuthenticatedView(
    onLoginButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.backup_settings_not_authenticated_description),
            fontSize = 16.sp,
            color = colorResource(R.color.primary_text),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginButtonClicked
        ) {
            Text(text = stringResource(R.string.backup_settings_authenticate_cta))
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.backup_settings_not_authenticated_description_2),
            fontSize = 16.sp,
            color = colorResource(R.color.secondary_text),
        )
    }
}