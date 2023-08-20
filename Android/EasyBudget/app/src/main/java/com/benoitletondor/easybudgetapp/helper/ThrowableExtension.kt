package com.benoitletondor.easybudgetapp.helper

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
        (depth <= IS_NETWORK_MAX_DEPTH && cause?.isNetworkError(depth + 1) ?: false)