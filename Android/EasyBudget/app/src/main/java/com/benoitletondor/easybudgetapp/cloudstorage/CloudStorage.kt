package com.benoitletondor.easybudgetapp.cloudstorage

import java.io.File

interface CloudStorage {
    suspend fun uploadFile(file: File, path: String)
    suspend fun uploadFile(file: File, path: String, progressListener: ((Double) -> Unit)?)
}