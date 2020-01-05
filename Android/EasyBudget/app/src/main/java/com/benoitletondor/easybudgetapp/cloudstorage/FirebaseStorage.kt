package com.benoitletondor.easybudgetapp.cloudstorage

import android.net.Uri
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.coroutines.Continuation
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

    override suspend fun getFileMetaData(path: String) = suspendCancellableCoroutine<FileMetaData?> { continuation ->
        val reference = storage.reference.child(path)

        reference.metadata.addOnSuccessListener { metadata ->
            continuation.resume(FileMetaData(path, Date(metadata.updatedTimeMillis)))
        }.addOnFailureListener { error ->
            if( error.isFileNotFoundError() ) {
                continuation.resume(null)
            } else {
                continuation.resumeWithException(error)
            }
        }
    }

    override suspend fun downloadFile(path: String, toFile: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val reference = storage.reference.child(path)

        reference.getFile(toFile).addOnSuccessListener {
            continuation.resume(Unit)
        }.addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }
    }

    override suspend fun deleteFile(path: String): Boolean = suspendCancellableCoroutine { continuation ->
        val reference = storage.reference.child(path)

        reference.delete().addOnSuccessListener {
            continuation.resume(true)
        }.addOnFailureListener { error ->
            if( error.isFileNotFoundError() ) {
                continuation.resume(false)
            } else {
                continuation.resumeWithException(error)
            }
        }
    }

}

private fun Throwable.isFileNotFoundError(): Boolean
    = this is StorageException && this.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND