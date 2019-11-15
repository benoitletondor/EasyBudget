package com.benoitletondor.easybudgetapp.helper

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.cloudstorage.FileMetaData
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.impl.DB_NAME
import com.benoitletondor.easybudgetapp.job.BackupJob
import com.benoitletondor.easybudgetapp.parameters.Parameters
import com.benoitletondor.easybudgetapp.parameters.saveLastBackupDate
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.TimeUnit

private const val BACKUP_JOB_REQUEST_TAG = "backuptag"

suspend fun backupDB(context: Context,
                     db: DB,
                     cloudStorage: CloudStorage,
                     auth: Auth,
                     parameters: Parameters): ListenableWorker.Result {
    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
    if( currentUser == null ) {
        Log.e(
            "BackupJob",
            "Not authenticated"
        )

        return ListenableWorker.Result.failure()
    }

    try {
        db.use { openedDB ->
            openedDB.triggerForceWriteToDisk()
        }
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error writing DB to disk",
            error
        )

        return ListenableWorker.Result.failure()
    }

    val copyFile = File(context.cacheDir, "db_backup_copy")
    try {
        context.getDatabasePath(DB_NAME)
            .copyTo(copyFile, overwrite = true)
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error copying DB",
            error
        )

        return ListenableWorker.Result.failure()
    }

    try {
        cloudStorage.uploadFile(copyFile, getDBPath(currentUser.id))
        parameters.saveLastBackupDate(Date())
    } catch (error: Throwable) {
        Log.e(
            "BackupJob",
            "Error uploading",
            error
        )

        return ListenableWorker.Result.retry()
    } finally {
        try {
            copyFile.delete()
        } catch (error: Throwable) {
            Log.e(
                "BackupJob",
                "Error deleting temp file",
                error
            )
        }
    }

    return ListenableWorker.Result.success()
}

suspend fun getBackupDBMetaData(cloudStorage: CloudStorage,
                                auth: Auth): FileMetaData {
    val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
        ?: throw IllegalStateException("Not authenticated")

    return cloudStorage.getFileMetaData(getDBPath(currentUser.id))
}

private fun getDBPath(userId: String): String {
    return "user/$userId/db.backup"
}

fun scheduleBackup(context: Context) {
    unscheduleBackup(context)

    val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val backupRequest = PeriodicWorkRequestBuilder<BackupJob>(7, TimeUnit.DAYS)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .setInitialDelay(1, TimeUnit.DAYS)
        .addTag(BACKUP_JOB_REQUEST_TAG)
        .build()

    WorkManager.getInstance(context).enqueue(backupRequest)
}

fun unscheduleBackup(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(BACKUP_JOB_REQUEST_TAG)
}

fun getBackupJobInfosLiveData(context: Context): LiveData<List<WorkInfo>> {
    return WorkManager.getInstance(context).getWorkInfosByTagLiveData(BACKUP_JOB_REQUEST_TAG)
}