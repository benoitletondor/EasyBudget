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
package com.benoitletondor.easybudgetapp.view.settings.backup.subviews

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
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
fun ColumnScope.NotAuthenticatedView(
    onLoginButtonClicked: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.backup_settings_not_authenticated_description),
        fontSize = 16.sp,
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