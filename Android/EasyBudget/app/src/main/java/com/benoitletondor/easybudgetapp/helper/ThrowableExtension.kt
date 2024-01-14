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

package com.benoitletondor.easybudgetapp.helper

import com.google.firebase.FirebaseNetworkException
import io.realm.kotlin.mongodb.exceptions.ConnectionException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private const val IS_NETWORK_MAX_DEPTH = 2

fun Throwable.isNetworkError(depth: Int = 0): Boolean =
    this is InterruptedIOException ||
        this is UnknownHostException ||
        this is SocketException ||
        this is SSLException ||
        this is ConnectionException ||
        this is FirebaseNetworkException ||
        (depth <= IS_NETWORK_MAX_DEPTH && cause?.isNetworkError(depth + 1) ?: false)