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

package com.benoitletondor.easybudgetapp.db.cacheimpl

import com.benoitletondor.easybudgetapp.db.onlineimpl.Account
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import com.benoitletondor.easybudgetapp.helper.Logger
import com.benoitletondor.easybudgetapp.model.DataForMonth
import kotlinx.coroutines.cancel
import java.time.YearMonth

class CachedOnlineDBImpl(
    private val wrappedDB: OnlineDB,
) : OnlineDB, CachedDBImpl(wrappedDB) {
    var isClosed: Boolean = false
        private set

    override val account: Account
        get() = wrappedDB.account

    override suspend fun deleteAllEntries() {
        wrappedDB.deleteAllEntries()
    }

    override suspend fun getDataForMonth(yearMonth: YearMonth): DataForMonth {
        if (isClosed) {
            Logger.error("Trying to access closed online DB for account: ${account.id}", IllegalStateException("DB is closed"))
            return DataForMonth(yearMonth, emptyMap())
        }

        return super.getDataForMonth(yearMonth)
    }

    override fun close() {
        isClosed = true
        cancel()
        wrappedDB.close()
    }
}