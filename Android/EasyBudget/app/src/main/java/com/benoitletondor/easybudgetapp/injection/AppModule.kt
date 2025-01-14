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

package com.benoitletondor.easybudgetapp.injection

import android.content.Context
import com.benoitletondor.easybudgetapp.BuildConfig
import com.benoitletondor.easybudgetapp.accounts.Accounts
import com.benoitletondor.easybudgetapp.accounts.FirebaseAccounts
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.CurrentUser
import com.benoitletondor.easybudgetapp.auth.FirebaseAuth
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.cloudstorage.FirebaseStorage
import com.benoitletondor.easybudgetapp.config.Config
import com.benoitletondor.easybudgetapp.config.FirebaseRemoteConfig
import com.benoitletondor.easybudgetapp.iab.Iab
import com.benoitletondor.easybudgetapp.iab.IabImpl
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.cacheimpl.CachedDBImpl
import com.benoitletondor.easybudgetapp.db.cacheimpl.CachedOnlineDBImpl
import com.benoitletondor.easybudgetapp.db.offlineimpl.OfflineDBImpl
import com.benoitletondor.easybudgetapp.db.offlineimpl.RoomDB
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDB
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlineDBImpl
import com.benoitletondor.easybudgetapp.db.onlineimpl.OnlinePGDBImpl
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.AppConfiguration
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideIab(
        @ApplicationContext context: Context,
    ): Iab = IabImpl(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): Auth = FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance())

    @Provides
    @Singleton
    fun provideAccounts(): Accounts = FirebaseAccounts(Firebase.firestore)

    @Provides
    @Singleton
    fun provideCurrentDBProvider(): CurrentDBProvider = CurrentDBProvider(activeDB = null)

    @Provides
    @Singleton
    fun provideCloudStorage(): CloudStorage = FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance().apply {
        maxOperationRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
        maxDownloadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
        maxUploadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
    })

    @Provides
    @Singleton
    fun provideDB(
        @ApplicationContext context: Context,
    ): DB = CachedDBImpl(
        OfflineDBImpl(RoomDB.create(context)),
    )

    @Provides
    @Singleton
    fun provideConfig(): Config = FirebaseRemoteConfig()

    private const val SHOULD_USE_MONGO = false // Make sure to add the exclude("io.ktor") in gradle when changing this
    // Make sure to also remove runningFold in MainViewModel
    // Make sure to also delete config.watchProMigratedToPgAlertMessage()

    private var app: App? = null
    private var usedOnlineDB: CachedOnlineDBImpl? = null

    suspend fun provideSyncedOnlineDBOrThrow(
        appContext: Context,
        currentUser: CurrentUser,
        auth: Auth,
        accountId: String,
        accountSecret: String,
        accountHasBeenMigratedToPg: Boolean,
        accounts: Accounts,
    ): OnlineDB {
        usedOnlineDB?.close()

        if (SHOULD_USE_MONGO) {
            val app = this.app ?: run {
                val createdApp = App.create(
                    AppConfiguration.Builder(BuildConfig.ATLAS_APP_ID)
                        .enableSessionMultiplexing(true)
                        .build()
                )

                this.app = createdApp
                createdApp
            }

            val db = CachedOnlineDBImpl(
                OnlineDBImpl.provideFor(
                    currentUser = currentUser,
                    accountId = accountId,
                    accountSecret = accountSecret,
                    app = app,
                ),
            )

            usedOnlineDB = db
            return db
        } else {
            val db = CachedOnlineDBImpl(
                OnlinePGDBImpl.provideFor(
                    currentUser = currentUser,
                    auth = auth,
                    accountId = accountId,
                    accountSecret = accountSecret,
                    appContext = appContext,
                    accounts = accounts,
                    accountHasBeenMigratedToPg = accountHasBeenMigratedToPg,
                )
            )

            usedOnlineDB = db
            return db
        }
    }
}
