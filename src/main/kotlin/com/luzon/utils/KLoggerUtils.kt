package com.luzon.utils

import mu.KLogger

fun KLogger.errorWithException(msg: String): Nothing = errorWithException(msg, RuntimeException(msg))
fun KLogger.errorWithException(msg: String, throwable: Throwable): Nothing {
    error(msg)
    throw throwable
}
