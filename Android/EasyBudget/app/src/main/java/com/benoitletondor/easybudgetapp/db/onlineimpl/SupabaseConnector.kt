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

package com.benoitletondor.easybudgetapp.db.onlineimpl

import co.touchlab.kermit.Logger
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.crud.CrudEntry
import com.powersync.db.crud.UpdateType
import com.powersync.db.runWrappedSuspending
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json

@OptIn(SupabaseInternal::class)
class SupabaseConnector(
    private val supabaseClient: SupabaseClient,
    private val powerSyncEndpoint: String,
    currentUser: CurrentUser,
    private val auth: Auth,
) : PowerSyncBackendConnector() {
    private var errorCode: String? = null

    private object PostgresFatalCodes {
        // Using Regex patterns for Postgres error codes
        private val FATAL_RESPONSE_CODES =
            listOf(
                // Class 22 — Data Exception
                "^22...".toRegex(),
                // Class 23 — Integrity Constraint Violation
                "^23...".toRegex(),
                // INSUFFICIENT PRIVILEGE
                "^42501$".toRegex(),
            )

        fun isFatalError(code: String): Boolean =
            FATAL_RESPONSE_CODES.any { pattern ->
                pattern.matches(code)
            }
    }

    init {
        require(
            supabaseClient.pluginManager.getPluginOrNull(Postgrest) != null,
        ) { "The Postgrest plugin must be installed on the Supabase client" }

        // This retrieves the error code from the response
        // as this is not accessible in the Supabase client RestException
        // to handle fatal Postgres errors
        supabaseClient.httpClient.httpClient.plugin(HttpSend).intercept { request ->
            val resp = execute(request)
            val response = resp.response
            if (response.status.value == 400) {
                val responseText = response.bodyAsText()

                try {
                    val error = Json { coerceInputValues = true }.decodeFromString<Map<String, String?>>(responseText)
                    errorCode = error["code"]
                } catch (e: Exception) {
                    Logger.e("Failed to parse error response: $e")
                }
            }
            resp
        }
    }


    private var currentUser: CurrentUser? = currentUser

    override fun invalidateCredentials() {
        com.benoitletondor.easybudgetapp.helper.Logger.debug(
            "SupabaseConnector",
            "Invalidating token"
        )

        currentUser = null
    }

    override suspend fun fetchCredentials(): PowerSyncCredentials {
        val currentUser = currentUser
        val token = if (currentUser == null) {
            com.benoitletondor.easybudgetapp.helper.Logger.debug(
                "SupabaseConnector",
                "Regenerating token"
            )
            auth.refreshUserTokens()
            val newUser = (auth.state.value as? AuthState.Authenticated)?.currentUser ?: throw IllegalStateException("User not authenticated")
            this.currentUser = newUser
            newUser.token
        } else {
            currentUser.token
        }

        return PowerSyncCredentials(
            endpoint = powerSyncEndpoint,
            token = token,
            userId = this.currentUser?.id,
        )
    }

    override suspend fun getCredentialsCached(): PowerSyncCredentials {
        return fetchCredentials()
    }

    override suspend fun prefetchCredentials(): Job? = null

    /**
     * Upload local changes to the app backend (in this case Supabase).
     *
     * This function is called whenever there is data to upload, whether the device is online or offline.
     * If this call throws an error, it is retried periodically.
     */
    override suspend fun uploadData(database: PowerSyncDatabase) {
        return runWrappedSuspending {
            val transaction = database.getNextCrudTransaction() ?: return@runWrappedSuspending

            var lastEntry: CrudEntry? = null
            try {
                for (entry in transaction.crud) {
                    lastEntry = entry

                    val table = supabaseClient.from(entry.table)

                    when (entry.op) {
                        UpdateType.PUT -> {
                            val data = entry.opData?.toMutableMap() ?: mutableMapOf()
                            data["id"] = entry.id
                            table.upsert(data)
                        }

                        UpdateType.PATCH -> {
                            table.update(entry.opData!!) {
                                filter {
                                    eq("id", entry.id)
                                }
                            }
                        }

                        UpdateType.DELETE -> {
                            table.delete {
                                filter {
                                    eq("id", entry.id)
                                }
                            }
                        }
                    }
                }

                transaction.complete(null)
            } catch (e: Exception) {
                if (errorCode != null && PostgresFatalCodes.isFatalError(errorCode.toString())) {
                    /**
                     * Instead of blocking the queue with these errors,
                     * discard the (rest of the) transaction.
                     *
                     * Note that these errors typically indicate a bug in the application.
                     * If protecting against data loss is important, save the failing records
                     * elsewhere instead of discarding, and/or notify the user.
                     */
                    Logger.e("Data upload error: ${e.message}")
                    Logger.e("Discarding entry: $lastEntry")
                    transaction.complete(null)
                    return@runWrappedSuspending
                }

                Logger.e("Data upload error - retrying last entry: $lastEntry, $e")
                throw e
            }
        }
    }
}
