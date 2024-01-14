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

package com.benoitletondor.easybudgetapp.cloudstorage

import android.net.Uri
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseStorage(private val storage: com.google.firebase.storage.FirebaseStorage) : CloudStorage {

    override suspend fun uploadFile(file: File, path: String) {
        uploadFile(file, path, null)
    }

    override suspend fun uploadFile(file: File, path: String, progressListener: ((Double) -> Unit)?) = suspendCancellableCoroutine { continuation ->
        val reference = storage.reference.child(path)

        reference.putFile(Uri.fromFile(file)).addOnSuccessListener {
            continuation.resume(Unit)
        }.addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }.addOnProgressListener { taskSnapshot ->
            progressListener?.invoke(taskSnapshot.bytesTransferred.toDouble() / taskSnapshot.totalByteCount.toDouble())
        }
    }

    override suspend fun getFileMetaData(path: String) = suspendCancellableCoroutine { continuation ->
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

    override suspend fun downloadFile(path: String, toFile: File) = suspendCancellableCoroutine { continuation ->
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