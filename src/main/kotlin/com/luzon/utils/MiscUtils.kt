package com.luzon.utils

import java.util.*

internal fun <T> Stack<T>.peekOrNull() = if (isEmpty()) null else peek()
internal fun <T> Stack<T>.popOrNull() = if (isEmpty()) null else pop()
