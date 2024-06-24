/*
 *   Copyright 2024 Benoit LETONDOR
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
package com.benoitletondor.easybudgetapp.compose

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

private val isAndroid33OrMore = Build.VERSION.SDK_INT >= 33

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberPermissionStateCompat(
    onPermissionResult: (Boolean) -> Unit,
) : PermissionState {
    return if (isAndroid33OrMore) {
        rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS,
            onPermissionResult = onPermissionResult,
        )
    } else {
        remember {
            object : PermissionState {
                override val permission: String = "android.permission.POST_NOTIFICATIONS"
                override val status: PermissionStatus = PermissionStatus.Granted
                override fun launchPermissionRequest() {
                    onPermissionResult(true)
                }
            }
        }
    }
}