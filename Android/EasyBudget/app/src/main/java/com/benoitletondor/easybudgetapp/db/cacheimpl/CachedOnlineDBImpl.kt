/*
 *   Copyright 2023 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.db.cacheimpl

import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor

class CachedOnlineDBImpl(
    private val wrappedDB: OnlineDB,
    cacheStorage: CacheDBStorage,
    executor: Executor,
) : OnlineDB, CachedDBImpl(
    wrappedDB, cacheStorage, executor
) {
    override val account: Account
        get() = wrappedDB.account

    override suspend fun deleteAllEntries() {
        wrappedDB.deleteAllEntries()

        wipeCache()
    }

    override fun close() {
        wrappedDB.close()
        wipeCache()
        cancel()
    }
}