package com.benoitletondor.easybudgetapp.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.helper.backupDB
import com.benoitletondor.easybudgetapp.parameters.Parameters
import org.koin.java.KoinJavaComponent.get

class BackupJob(private val context: Context,
                workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val db: DB = get(DB::class.java)
    private val cloudStorage: CloudStorage = get(CloudStorage::class.java)
    private val auth: Auth = get(Auth::class.java)
    private val parameters: Parameters = get(Parameters::class.java)

    override suspend fun doWork(): Result {
        return backupDB(context, db, cloudStorage, auth, parameters)
    }

}