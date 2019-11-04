package com.benoitletondor.easybudgetapp.cloudstorage

import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseStorage(private val storage: com.google.firebase.storage.FirebaseStorage) : CloudStorage {

    override suspend fun uploadFile(file: File, path: String) {
        uploadFile(file, path, null)
    }

    override suspend fun uploadFile(file: File, path: String, progressListener: ((Double) -> Unit)?) = suspendCancellableCoroutine<Unit> { continuation ->
        val reference = storage.reference.child(path)

        reference.putFile(Uri.fromFile(file)).addOnSuccessListener {
            continuation.resume(Unit)
        }.addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }.addOnProgressListener { taskSnapshot ->
            progressListener?.invoke(taskSnapshot.bytesTransferred.toDouble() / taskSnapshot.totalByteCount.toDouble())
        }
    }

}