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