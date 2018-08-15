package com.luzon.utils

import mu.KLogger

internal fun KLogger.errorWithException(msg: String): Nothing = errorWithException(msg, RuntimeException(msg))
internal fun KLogger.errorWithException(msg: String, throwable: Throwable): Nothing {
    error(msg)
    throw throwable
}