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
package com.benoitletondor.easybudgetapp.helper

import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.PremiumCheckStatus
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.getLastBackupDate
import com.benoitletondor.easybudgetapp.parameters.isBackupEnabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class OfflineAccountBackupStatus {
    data object Unavailable: OfflineAccountBackupStatus()
    data object Disabled: OfflineAccountBackupStatus()
    data class Enabled(val authState: AuthState, val lastBackupDaysAgo: Long?): OfflineAccountBackupStatus()
}

fun getOfflineAccountBackupStatusFlow(
    iab: Iab,
    parameters: Parameters,
    auth: Auth,
): Flow<OfflineAccountBackupStatus> {
    return iab.iabStatusFlow
        .flatMapLatest { iabStatusFlow ->
            when(iabStatusFlow) {
                PremiumCheckStatus.INITIALIZING,
                PremiumCheckStatus.CHECKING,
                PremiumCheckStatus.ERROR,
                PremiumCheckStatus.NOT_PREMIUM -> return@flatMapLatest flowOf(OfflineAccountBackupStatus.Unavailable)
                PremiumCheckStatus.LEGACY_PREMIUM,
                PremiumCheckStatus.PREMIUM_SUBSCRIBED,
                PremiumCheckStatus.PRO_SUBSCRIBED -> {
                    if (parameters.isBackupEnabled()) {
                        val lastBackupDate = parameters.getLastBackupDate()
                        val backupDiffDaysValue = backupDiffDays(lastBackupDate)

                        return@flatMapLatest auth.state
                            .mapLatest { authState ->
                                OfflineAccountBackupStatus.Enabled(authState, backupDiffDaysValue)
                            }
                    } else {
                        return@flatMapLatest flowOf(OfflineAccountBackupStatus.Disabled)
                    }
                }
            }
        }
}

private fun backupDiffDays(lastBackupDate: Date?): Long? {
    if (lastBackupDate == null) {
        return null
    }

    val now = Date()
    val diff = now.time - lastBackupDate.time
    val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

    return diffInDays
}