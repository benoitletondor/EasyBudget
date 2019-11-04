package com.benoitletondor.easybudgetapp.job

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.benoitletondor.easybudgetapp.auth.Auth
import com.benoitletondor.easybudgetapp.auth.AuthState
import com.benoitletondor.easybudgetapp.cloudstorage.CloudStorage
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.impl.DB_NAME

import org.koin.java.KoinJavaComponent.get
import java.io.File

class BackupJob(private val context: Context,
                workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val db: DB = get(DB::class.java)
    private val cloudStorage: CloudStorage = get(CloudStorage::class.java)
    private val auth: Auth = get(Auth::class.java)

    override suspend fun doWork(): Result {
        val currentUser = (auth.state.value as? AuthState.Authenticated)?.currentUser
        if( currentUser == null ) {
            Log.e(
                "BackupJob",
                "Not authenticated"
            )

            return Result.failure()
        }

        try {
            db.use { db ->
                db.triggerForceWriteToDisk()
            }
        } catch (error: Throwable) {
            Log.e(
                "BackupJob",
                "Error writing DB to disk",
                error
            )

            return Result.failure()
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

            return Result.failure()
        }

        try {
            cloudStorage.uploadFile(copyFile, "user/${currentUser.id}/db.backup")
        } catch (error: Throwable) {
            Log.e(
                "BackupJob",
                "Error uploading",
                error
            )

            return Result.retry()
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

        return Result.success()
    }


}