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
fun rememberPermissionStateCompat() : PermissionState {
    return if (isAndroid33OrMore) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        remember {
            object : PermissionState {
                override val permission: String = "android.permission.POST_NOTIFICATIONS"
                override val status: PermissionStatus = PermissionStatus.Granted
                override fun launchPermissionRequest() {
                    /* No-op */
                }
            }
        }
    }
}