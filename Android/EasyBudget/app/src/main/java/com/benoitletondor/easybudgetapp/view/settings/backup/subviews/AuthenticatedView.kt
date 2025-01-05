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

import android.text.format.DateUtils
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.compose.components.LoadingView
import com.benoitletondor.easybudgetapp.view.settings.backup.BackupSettingsViewModel

@Composable
fun ColumnScope.AuthenticatedView(
    state: BackupSettingsViewModel.State.Authenticated,
    onLogoutButtonClicked: () -> Unit,
    onBackupActivationChange: (Boolean) -> Unit,
    onBackupNowClicked: () -> Unit,
    onRestoreNowClicked: () -> Unit,
    onDeleteBackupClicked: () -> Unit,
) {
    val context = LocalContext.current

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

    when(state) {
        is BackupSettingsViewModel.State.Activated,
        is BackupSettingsViewModel.State.NotActivated -> {
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onLogoutButtonClicked,
            ) {
                Text(text = stringResource(R.string.backup_settings_logout_cta))
            }

            Spacer(modifier = Modifier.height(30.dp))

            when(state) {
                is BackupSettingsViewModel.State.Activated -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.backup_settings_cloud_backup_status, stringResource(R.string.backup_settings_cloud_backup_status_activated)),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = true,
                            onCheckedChange = onBackupActivationChange,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.backup_activated_description),
                        fontSize = 15.sp,
                        color = colorResource(R.color.secondary_text),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val lastBackUpDateText = remember(state.lastBackupDate) {
                        if( state.lastBackupDate != null ) {
                            val timeFormatted = DateUtils.getRelativeDateTimeString(
                                context,
                                state.lastBackupDate.time,
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.WEEK_IN_MILLIS,
                                DateUtils.FORMAT_SHOW_TIME
                            )

                            context.getString(R.string.backup_last_update_date, timeFormatted)
                        } else {
                            context.getString(R.string.backup_last_update_date, context.getString(R.string.backup_last_update_date_never))
                        }
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = lastBackUpDateText,
                        fontSize = 16.sp,
                    )

                    if (state.backupNowAvailable) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onBackupNowClicked,
                        ) {
                            Text(text = stringResource(R.string.backup_now_cta))
                        }
                    }

                    if (state.restoreAvailable) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.backup_restore_description),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.backup_restore_explanation),
                            fontSize = 15.sp,
                            color = colorResource(R.color.secondary_text),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onRestoreNowClicked,
                        ) {
                            Text(text = stringResource(R.string.backup_restore_cta))
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.backup_wipe_data_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.backup_wipe_data_description),
                            fontSize = 15.sp,
                            color = colorResource(R.color.secondary_text),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onDeleteBackupClicked,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorResource(R.color.budget_red)
                            ),
                        ) {
                            Text(text = stringResource(R.string.backup_wipe_data_cta))
                        }
                    }
                }
                is BackupSettingsViewModel.State.NotActivated -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.backup_settings_cloud_backup_status, stringResource(R.string.backup_settings_cloud_backup_status_disabled)),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = false,
                            onCheckedChange = onBackupActivationChange,
                        )
                    }
                }
                else -> {}
            }
        }
        is BackupSettingsViewModel.State.BackupInProgress -> LoadingView()
        is BackupSettingsViewModel.State.DeletionInProgress -> LoadingView()
        is BackupSettingsViewModel.State.RestorationInProgress -> LoadingView()
    }
}