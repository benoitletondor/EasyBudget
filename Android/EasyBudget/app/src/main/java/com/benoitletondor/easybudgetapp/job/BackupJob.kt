package com.benoitletondor.easybudgetapp.job

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.benoitletondor.easybudgetapp.db.DB
import com.benoitletondor.easybudgetapp.db.impl.DB_NAME

import org.koin.java.KoinJavaComponent.get
import java.io.File

class BackupJob(private val context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val db: DB = get(DB::class.java)

    override suspend fun doWork(): Result {
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

            return Result.retry()
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

            return Result.retry()
        }

        

        return Result.success()
    }


}